# jFAST
This repository contains the first version of the org.slf4g.impl.RingBufferLogger, extension of the SimpleLogger.  This class stores the log messages in the internal or external RingBuffer. By default this logger will read messages from the internal ring buffer (in separate thread) and write them to the standard error output stream (as the SimpleLogger does). To instantiate the RingBufferLogger use the following chain of methods:
Logger logger = StaticLoggerBinder.getSingleton().getLoggerFactory().getLogger(name), 
where name is a string to identify an instance of the logger.
Useful tips:
1.	If you wish to avoid writing messages to a ring buffer completely (messages will go directly to the specified output file) then just call 
logger.setRing(null);
2.	If you wish to handle the log messages by yourself after they are extracted from the ring buffer then provide the RingBufferLoggerMessageConsumer myConsumer, and set it in the logger: 
logger.setConsumer(myConsumer);
3.	If you wish to use your own ring buffer to store and retrieve log messages, then create your own ring and be responsible for it:
RingBuffer myRing = new RingBuffer(…);
logger.setRing(myRing);
logger.setConsumer(null);
// Start a thread to read from myRing.
Note: All these cases are represented in the RingBufferLoggerTest class.

