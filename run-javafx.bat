@echo off
title Research Publication Verification & Management System (JavaFX + MySQL)
echo ==============================================================================
echo Starting Research Publication Verification & Management System (JavaFX + JDBC)
echo ==============================================================================

set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.6.2\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%~dp0"
"C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.6.2\plugins\maven\lib\maven3\bin\mvn.cmd" javafx:run
pause
