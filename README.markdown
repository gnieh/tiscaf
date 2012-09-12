tiscaf
======

Introduction
------------

tiscaf is an http server written in and intended to be used with the Scala programming language. 

This project is based on the original work by Andrew Gaydenko (http://gaydenko.com/scala/tiscaf/httpd/) in its version 0.7. This explains why the software version starts at 0.8 in this repository.
Many thanks to him for his help and answers.

Motivations
-----------

Very often a current software exosphere makes almost impossible to do things a simple way. Almost all developers are under heavy pressure of dependency on J2EE and accompanying frameworks with dozens of external libraries (how many megabytes do those external jars take?). All you know these software monsters with infinite dependencies trees... I have decided I need more freedom.

OTOH, if you like to spend your life dealing with extraneous-designed XML-files (rather coding) and/or to satisfy multiple APIs presenting good ideas (even if last ones are indeed good), be warned - tiscaf lives in another world.

The server has just what you need to handle an HTTP request, no more. I treat the server as a low-level self-contained "assembly" module (being used instead of servlet engine) which is easy to embed into any application as well as to wrap with, say, own templating framework, with any dispatching model (besides included helper for tree-like URIs space), with this or that (besides default) execution environment (say, Comet-like), and so on. 

Features
--------

 - nio - nio using permits resources managing.
 - dynamics - besides default execution pool it is possible to use own execution environment for each request handler.
 - full streaming - the server allows a streaming in both directions; say, you can download and upload ISO images.
 - dispatching - requests dispatching is limited with your imagination only.
 - multiport - listening to multiple ports is possible.
 - static content - file system and in-jar resources retrieving is supported out of the box (with files/directories browsing).
 - connections - persistent connections are supported.
 - output - raw (content-length is known), buffered, gzipped (as a case of buffered), chunked - all modes are supported.
 - methods - POST/urlencoded, POST/multipart (with falling POST back to octets), GET and DELETE methods are supported.
 - sessions - via URI path extensions or cookies are supported.
 - config files - are absent.
 - depends on - nothing (again: nothing).
 - licensing - [LGPL](http://www.gnu.org/licenses/lgpl.html).

Upcoming Features and TODOs
---------------------------

 - suspendable computation - HLet computation may be interrupted at any moment and resumed later
 - SSL support - TODO
