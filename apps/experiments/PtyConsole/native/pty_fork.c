/*
 * Copyright (c) 2010, 2014 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

// Copied from NetBeans: dlight.nativeexecution/tools/pty/src/pty_fork.c
// with some minor changes to ensure warning-free compilation.

#define _XOPEN_SOURCE 600
#include "pty_fork.h"
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <termios.h>
#include <stdarg.h>
#ifdef SOLARIS
#include <stropts.h>
#else
#include <sys/ioctl.h>
#endif
#include <errno.h>
#include <fcntl.h>
#include <signal.h>

#ifdef __CYGWIN__
//added for compatibility with cygwin 1.5

int posix_openpt(int flags) {
    return open("/dev/ptmx", flags);
}

#endif
extern int grantpt(int);
extern int unlockpt(int);
extern char *ptsname(int);
extern void err_sys(const char *fmt, ...);
static void dup_fd(int pty_fd);
static int ptm_open(void);
static int pts_open(int masterfd);

int ptm_open(void) {
    int masterfd;

    /*
     * O_NOCTTY (?) we'll be a group leader in any case (?)
     * So will get a controlling terminal.. O_NOCTTY - do we need it?
     *
     */
    if ((masterfd = posix_openpt(O_RDWR | O_NOCTTY)) == -1) {
        return -1;
    }

    if (grantpt(masterfd) == -1 || unlockpt(masterfd) == -1) {
        close(masterfd);
        return -1;
    }

    return masterfd;
}

int pts_open(int masterfd) {
    int slavefd;
    char* name;

    if ((name = ptsname(masterfd)) == NULL) {
        close(masterfd);
        return -1;
    }

    if ((slavefd = open(name, O_RDWR)) == -1) {
        close(masterfd);
        return -1;
    }

#if defined (__SVR4) && defined (__sun)
    if (ioctl(slavefd, I_PUSH, "ptem") == -1) {
        close(masterfd);
        close(slavefd);
        return -1;
    }

    if (ioctl(slavefd, I_PUSH, "ldterm") == -1) {
        close(masterfd);
        close(slavefd);
        return -1;
    }

    if (ioctl(slavefd, I_PUSH, "ttcompat") == -1) {
        close(masterfd);
        close(slavefd);
        return -1;
    }
#endif

    return slavefd;
}

pid_t pty_fork(int *ptrfdm) {
    pid_t pid;
    char* name;
    int master_fd, pty_fd;
    struct termios termios;
    struct termios* ptermios = NULL;
    struct winsize wsize;
    struct winsize* pwinsize = NULL;

    // If we are in a terminal - get its params and set them to
    // a newly allocated one...

    if (isatty(STDIN_FILENO)) {
        ptermios = &termios;
        if (tcgetattr(STDIN_FILENO, ptermios) == -1) {
            err_sys("tcgetattr failed");
        }

        pwinsize = &wsize;
        if (ioctl(STDIN_FILENO, TIOCGWINSZ, pwinsize) == -1) {
            err_sys("ioctl(TIOCGWINSZ) failed");
        }
    }

    if ((master_fd = ptm_open()) < 0) {
        err_sys("ERROR: ptm_open() failed [%d]\n", master_fd);
    }

    if ((name = ptsname(master_fd)) == NULL) {
        close(master_fd);
        return -1;
    }

    // Put values to the output params
    *ptrfdm = master_fd;

    if ((pid = fork()) < 0) {
        err_sys("fork failed");
        return (-1);
    }

    if (pid == 0) { /* child */
        if (setsid() < 0) {
            err_sys("setsid error");
        }

        if ((pty_fd = pts_open(master_fd)) < 0) {
            err_sys("can't open slave pty");
        }

        if (ptermios != NULL) {
            if (tcsetattr(pty_fd, TCSANOW, ptermios) == -1) {
                err_sys("tcsetattr(TCSANOW) failed");
            }
        }

        if (pwinsize != NULL) {
            if (ioctl(pty_fd, TIOCSWINSZ, pwinsize) == -1) {
                err_sys("ioctl(TIOCSWINSZ) failed");
            }
        }

        close(master_fd);
        dup_fd(pty_fd);

        return (0); /* child returns 0 just like fork() */
    } else { /* parent */
        return (pid); /* parent returns pid of child */
    }

}

pid_t pty_fork1(const char *pty) {
    pid_t pid;
    int pty_fd;

    if ((pid = fork()) < 0) {
        printf("FAILED");
        return (-1);
    }

    if (pid == 0) { /* child */
        /*
         * Create a new process session for this child.
         */
        if (setsid() < 0) {
            err_sys("setsid error");
        }

        /*
         * Open a terminal descriptor...
         */
        if ((pty_fd = open(pty, O_RDWR)) == -1) {
            err_sys("ERROR cannot open pty \"%s\" -- %s\n",
                    pty, strerror(errno));
        }

        /*
         * Associate pty_fd with I/O and close it
         */
        dup_fd(pty_fd);
        return (0);
    } else {
        /*
         * parent just returns a pid of the child
         */
        return (pid);
    }
}

static void dup_fd(int pty_fd) {
    // Ensure SIGINT isn't being ignored
    struct sigaction act;
    sigaction(SIGINT, NULL, &act);
    act.sa_handler = SIG_DFL;
    sigaction(SIGINT, &act, NULL);


#if defined(TIOCSCTTY) && !defined(__sun) && !defined(__APPLE__)
    if (ioctl(pty_fd, TIOCSCTTY, 0) == -1) {
        printf("ERROR ioctl(TIOCSCTTY) failed on \"pty %d\" -- %s\n",
                pty_fd, strerror(errno));
        exit(-1);
    }
#endif

    /*
     * Slave becomes stdin/stdout/stderr of child.
     */
    if (dup2(pty_fd, STDIN_FILENO) != STDIN_FILENO) {
        err_sys("dup2 error to stdin");
    }

    if (dup2(pty_fd, STDOUT_FILENO) != STDOUT_FILENO) {
        err_sys("dup2 error to stdout");
    }

    if (dup2(pty_fd, STDERR_FILENO) != STDERR_FILENO) {
        err_sys("dup2 error to stderr");
    }

    close(pty_fd);
}
