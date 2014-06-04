WebTerminal implements a general-purpose interactive console:
Users can type commands which get sent to a command handler,
which evaluates the command, and displays the results, typically
in some kind of type-script format.

Applications of WebTerminal include:
+ A chat/talk window.
+ A read-eval-print-loop for an interactive scripting language.
+ A command console.
+ A terminal emulator.

WebTerminal used as a terminal emulator recognizes a subset of the
standard ANSI/xterm terminal escape sequences for repositioning the
cursor, erasing previous text, changing style, etc.  A large enough
subset is recognized that you can run programs like the emacs text
editor, with styling.  This requires you specify the correct terminal
type (TERM), which is done automatically by the PtyTerminal
application.  A matching terminfo entry is provided, but it also
should work ok to specify TERM to a standard terminal name, like
eterm-color, vte, gnome, konsole, and xterm (all with color),
or vt100 or vt220 (both non-color).

The terminfo directory defines the escape sequences supported by the
"jfxterm" terminal type, using a terminfo descriptor.  The terminal emulation
is a subset of the standard xterm/ansi/vt100 ones.

There is an escape sequence to send HTML to WebTerminal.
This allows you to embed images, forms, and other HTML elements
in the console output.

The primary class is webterminal.WebTerminal, which is
an abstract (overridable) control you embed in your scenegraph.

The utility class webterminal.RunInConsole make it easy to
run an old-fashioned console-based Java application in a WebTerminal.
It re-binds System.in, System.out, and System.err to the WebTerminal.

For example, the jrunscript tool is a wrapper around the class
com.sun.tools.script.shell.Main in tools.jar.  So to start up
a JavaScript read-elavl-print-tool, do:

  $ ant run-main -Drunmain.classpath=$JAVA_HOME/lib/tools.jar \
  -Drunmain.class="com.sun.tools.script.shell.Main" \
  -Drunmain.args="-l javascript"

The application webterminal.ShellConsole uses java.lang.Process
to run a process inside a WebTerminal.  Currently, it is hard-wired to
run /bin/bash.  (It does work under Cygwin if you edit the
commandWithArgs field.)

Other example applications are in the separate WebTerminalApps directory.
