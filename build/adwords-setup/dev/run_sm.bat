@echo off
set startTime=%time%

REM
REM Define the output folder path
REM
set webDir="C:/Program Files/Zend/Apache2/htdocs"
set csvDir="C:/temp/adwords"

REM
REM Propagate the template one city at a time
REM
main.py -w %webDir% -c %csvDir%

echo.
echo -- Started at:  %startTime%
echo -- Finished at: %time%
echo.
