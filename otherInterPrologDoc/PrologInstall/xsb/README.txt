To build XSB:

* Mac
** Define JAVA_HOME
** Follow the xsb manual

* Windows (native)
** Get VC++ 10
** Microsoft SDK installed; this shoul dbe necessary only for 64bit 
** Open console with VC environment vars
** Place custom_settings.mak in build\windows or windows64, adapting to your context, including pthreads lib FILE, not directory
** pthreadVC2 installed
** Follow the xsb manual
* Windows (cygwin dependent; cygwin independent configure seems broken for recent cygwin)
** $ ./configure --disable-no-cygwin
** To run xsb afterwards add c:\cygwin\bin to PATH (because of cygwin1.dll)

* If running on VirtualBox, start from a fresh Windows directory, as sometimes configure will erroneously create some Mac config directories

* Ibidem for Flora
** ...but keep XSB in fidjiXSB besides the Flora directory