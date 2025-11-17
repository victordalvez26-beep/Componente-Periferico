PostgreSQL JDBC driver

Este directorio contiene utilities para obtener el driver JDBC de PostgreSQL.

Uso (PowerShell):

    # Ejecuta el script para descargar la versión 42.2.14
    .\get-postgres-driver.ps1

El script descarga por defecto desde Maven Central:
https://repo1.maven.org/maven2/org/postgresql/postgresql/42.2.14/postgresql-42.2.14.jar

Si tu entorno no permite descarga directa, descarga manualmente el JAR y colócalo en este directorio con nombre:
`postgresql-42.2.14.jar`
