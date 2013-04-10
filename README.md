# jbpm-more-examples

This project contains examples of various jBPM processes (both 5 and 6). 

### Examples
At the moment, the project contains examples that show how to

- handle exceptions in your (BPMN2) workflow using jBPM 
  - (see the [exception-handling](exception-handling) module)
- use process variables in various ways 
  - (see the [process-variables](process-variables) module)

### Versions of jBPM
Examples for various branches are stored on *different branches* of this project. 

For example, if you're looking for examples for the 5.4.x versions of jBPM, you can find them on the
5.4.x branch of this project. 

Currently, this project contains examples for the following versions of the jBPM: 
- [6.0.x](.)
- [5.4.x](../../tree/5.4.x)
- [5.2.x](../../tree/5.2.x)

# Getting Started

1. Clone this repository.
  - `git clone git@github.com:mrietveld/jbpm-more-examples.git jbpm-more-examples`
2. Change into the directory of the repository and build it.
  - `cd jbpm-more-examples`
  - `mvn clean install -DskipTests` 
3. Look through the tests to find an example that you're looking for. 

# Currently available tests: 

### Exception Handling

- [CatchIntermediateEscalationTest](exception-handling/src/test/java/org/jbpm/more/examples/CatchIntermediateErrorTest.java)
  - Shows how to handle exceptions using a Throw Event in a SubProcess (to which a Boundary Event is
    attached.) 

### Process Variables
