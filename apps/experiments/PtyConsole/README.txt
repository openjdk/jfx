An application that implements a terminal emulator
using WebTermnal and a PTY to fork off an operating system process.

The TERM and TERMINFO environment variables are set up automatically
to use the "jfxterm" terminal type.

PtyConsole does depend on native code that assumes modern Unix-style
pseudo-teletypes. At the time of writing it has only been tested on
GNU/Linux Fedora 20; it may need some porting effort on older
or non-Linux platforms, especially Windows.
