@echo off
::set pa=%cd%
::set confPath=%pa%/classes/*
::set libPath=%pa%/lib/*
::set logPath=%pa%/logs
::set rcp=.;%confPath%;%libPath% -Xms256m -Xmx1024m -Dlogs.path=%logPath% cn.com.test.my12306.my12306.My12306Application
::echo %confPath%
::echo %libPath%
::echo %rcp%
::java -cp %rcp%
java -jar my12306-0.0.1-SNAPSHOT.jar