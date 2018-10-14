[![Build Status](https://circleci.com/gh/cvgaviao/osgi-subsystem-maven-plugin.svg?style=svg)](https://circleci.com/gh/cvgaviao/osgi-subsystem-maven-plugin)


OSGi Subsystem Archive Generator Maven Plugin
================================

A maven plugin that is aimed to generate an OSGi Subsystem compacted archive (.esa) containing a generated manifest file and, when required, its constituents derived from the project's dependencies defined in the POM file.

------------
### Documentation, download and usage instructions details

Full usage details, FAQs and examples are available on the
**[project documentation website](http://cvgaviao.github.io/osgi-subsystem-maven-plugin/index.html)**.

## Development

### Building

To build and run the tests, you will need Java 8 or later and Maven 3.5.4 or later. 
Simply clone this repository and run `mvn clean install`

In order to run the build with test coverage support then run `mvn clean install -Dc8tech.build.test.coverage`

#### Using Eclipse IDE + m2e

You can use the Eclipse IDE to develop and build the project.

Just import the project into a workspace. Eclipse will automatically ask you to install the m2e related plugins and extensions.

Once imported, you will be able to see the provided m2e launcher files in the run menu.


### Contributing
Note that the tests run the plugin against a number of sample test projects, located in the `test-projects` folder.
If adding new functionality, or fixing a bug, it is recommended that a sample project be set up so that the scenario
can be tested end-to-end.
See also [CONTRIBUTING.md](CONTRIBUTING.md) for information on deploying to Nexus and releasing the plugin.

