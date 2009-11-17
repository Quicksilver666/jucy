
New Features and Changes

     * Support for chaining proxies have been added. Now you can run this
       proxy behind the firewall. It is now possible to connect through
       multiple proxies. That is this proxy server can use another proxy
       to connect to another to connect to another ... so on and only
       then to the required host.
     * Some bugs have been fixed.
     * This server now works with ICQ correctly.
       
How to run server

   First go to the bin directory, and make changes to configuration file
   socks.properties, there are plenty comments there to give you some
   idea of what does what. Most important is to set range and users
   attributes. Range controls what hosts are allowed to use this proxy.
   And users attribute controls who is allowed to connect from those
   host. Not setting users implies that authentication will be done
   solely on the basis of the origin of the connection. If however users
   attribute is set, authentication will be based on the user name
   information which will be collected from the Identd daemon, so proxy
   users will require to run Identd daemons on their machines, in order
   to be granted permission to use proxy. Other attributes control proxy
   behaviour like timeouts and have reasonable default values.
   It is possible to allow user A,B and C to connect from machine M1 and
   users D and E to connect from machines M2 and M3, in order to achieve
   this you will have to define two configuration files, first would
   contain all initialisation data like port to listen on, timeouts to
   use and so on plus it will contain lines
   
   
     range = M1
     users = A;B;C
     
   Second file would contain just two lines + any comments
   
     range = M2;M3
     users = D;E
     
   You will then specify these files on the command line for SOCKS
   server.
   
   Once configuration is complete you should edit batch file to make it
   suitable for your machine, examples for UNIX and Windows machines are
   provided. If you are on UNIX do not forget to do chmod on the SOCKS
   file.
   
SocksEcho

   SocksEcho is another application which uses socks package. It is GUI
   echo client, which is SOCKS aware, you can make and accept connections
   through the proxy using this client. You can also send datagrams
   through SOCKS5 proxy. See SocksEcho.bat or SocksEcho on how to run it.
   
   
Requirements

   In order to run these application you will need Java Virtual Machine
   which supports Java 1.1 or higher. I have tested it with jdk1.1.6 and
   jdk1.2.2 under Windows95 and on jdk1.1.5 under Solaris. Server
   application might be able to run even under java 1.0, but I haven't
   tested this.
