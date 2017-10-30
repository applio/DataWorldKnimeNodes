# DataWorldKnimeNodes
data.world KNIME nodes providing access to the [data.world](https://data.world) platform and data hosted there from the [KNIME](http://knime.org) data mining and visualization software.

### Dependencies
* Included with this package is the data.world JDBC driver jar which can be obtained via [http://search.maven.org/](http://search.maven.org/#search%7Cga%7C1%7Cdw-jdbc) (grab the shaded.jar link).  Initially this project was developed against v0.4.1 of that driver, accessible via this direct [link](http://search.maven.org/remotecontent?filepath=world/data/dw-jdbc/0.4.1/dw-jdbc-0.4.1-shaded.jar).
* Not included with this package are dependencies on libraries supplied as part of the KNIME Core package.  Though not a complete list, this includes dependencies such as the [Apache HttpComponents](https://hc.apache.org/) project jars and the [Google Gson](https://github.com/google/gson) project jar.

## Authors
* Appliomics, LLC (Davin Potts)
