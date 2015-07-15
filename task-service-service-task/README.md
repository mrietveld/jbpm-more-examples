
## Handling Exceptions

This module contains a number of examples for handling exceptions in your flow. 

### Keep exceptions out of the engine

To start with, you'll always have more control over your process if you limit places where
exceptions can happen to your own code. 

In other words, any services or WorkItemHandler implementations that you use in your process should
always contain exception handling code (try/catch/finally) that prevents an exception happening
*inside* your code from being thrown into the engine, which will be executing your code. If you
don't understand what I mean by that, don't worry, I'll come back to it in a second.

#### How to call services or other classes from a (jBPM) BPMN2 process

When calling a service or other java implementation class from BPMN2, you have to declare the
service first. Your service declaration will look something like this: 

```xml
  <itemDefinition id="message-type" />
  <message id="service-message" itemRef="message-type" />
  <interface id="service-interface" name="com.company.ServiceImplementation">
    <operation id="service-operation" name="doThatThing">
      <inMessageRef>service-message</bpmn2:inMessageRef>
    </operation>
  </interface>
```

In the example above, `com.company.ServiceImplementation` has a public `doThatThing` method. It
accepts one argument. 


