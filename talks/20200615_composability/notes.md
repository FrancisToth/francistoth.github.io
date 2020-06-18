## Referential Transparency

```java
final int i = 1 + 2
final int j = add1(2)

final int k = i + j
// can be translated to
final int k = 1 + 2 + add1(2)

System.out.println(k);
```
- To be composed, an **expression** has to be **referentially transparent**
- This is the case if the expression can be replaced by the value it produces without affecting the program's output

---
## Composition fundamentals

```java
int i     = 1 + 2
boolean b = true && false
```

- Composition is about maintaining guarantees
- How does composition provide guarantees (by making functions pure and total)
- this ability makes order of evaluation irrelevant, provides local reasoning, 

- how to achieve determinism? what about side-effects?
- next, we'll talk about composition rules and patterns

- Composition is the ability to combine two **values** into one
- It guarantees that a component resulting from a combination can be used in any context
- This implies having **primitives** (eg. `int`) and **operators** (eg. `+`, `-`, `*`, ...)

Note:
- Let's elaborate our definition of composition
- By composition, we mean the ability to combine two values of the same type to produce another one.
- values are obtained by primitives and can be combined using operators, but we'll tackle that a bit later

Note:

- Immutability guarantees that a component resulting from a combination can be used in any context
- It enables **local reasoning**, that is the ability to reason about code without knowing how it works.

---
## Composition fundamentals
```java
final int i = 42;
// ...
final int j = i + 1; // i could be replaced by 42
System.out.println(j);
```
```java
public final int add1(int counter) {
  return counter + 1;
}
```
- The first main fundamental of composition is **local reasoning**
- It's the ability to reason about an expression without looking at how it is computed
- **Local reasoning** is the main principle of abstraction


- Composition results in immutable and pure data. 
- This has the following benefits among others:
  - refactoring
  - higher abstraction
  - **local reasoning**

Note:
- refactoring: substitution
- higher abstraction: no need to look at the implementation
- local reasoning: can reason only by looking at the function's signature, no need to look at the implementation details
  which is the main fundamental of abstraction, and which gives you the ability to safely compose big blocks from
  smaller ones.
- The first requirement for composition is 

---
## Side-Effects

```java
User findById(long userId) { /* ... */ }
Address extractCity(User user) { /* ... */ }

Address address = extractCity(findById(1));

System.out.println(address);
```
- Side-effects
  - have implicit post/pre-conditions and invariants
  - prevent local reasoning
  - disallow call to a function in multiple arbitrary contexts
  - ...but are always required to a certain extent

Note:
- Side-effects prevent composition mostly because they tie a function to a calling context
- This context regroups all the implicit post/pre conditions and invariants implied by running the function / statement
- In consequence, it gets impossible to reason about it only by looking at the function's signature, or put differently by reasoning about it locally.
- Instead, the caller has to think about dealing with potential runtime exceptions or nulls, watch out for the order in which each instruction is evaluated, and picture the state of the system at runtime to reason about the code.
- Without local reasoning we loose the ability to abstract over a function and to compose it with other functions, or put differently to compose simple blocks into more sophisticated ones.
- If you think about it, local reasoning is not a luxury. It's something that is critical when it comes to design complex systems.
- There is one problem however. Not matter the program you write, side-effects will be required at some point.


- the main problem is that if you don't know what a function does, combining with another one may have even more 
unexpected behavior, which potentially will affect deeply the 

// These two functions cannot be composed without
// multiple error/null check, global state initialization, etc...
// No Refactoring/Substitution possible without 
// affecting the program's output
// create a function which once passed the id find the user and extract the city.

// We must ensure purity and then composability

---
## Statements VS Values

```java
System.out.println("Please enter your name.");
String name = System.console().readLine();
System.out.println("Hi " + name + "!");
```
- In contrast with a value, a **statement**:
 - depends on the order of evaluation
 - is eager
 - is not deterministic
 - and context dependent

Note:
- In contrast with a value, a statement depends on the evaluation order. We cannot switch the order of two statements
  without potentially affecting the result of program's output.
- Secondly a statement is eager. It executes as soon as it has been declared and cannot be passed around just like any other
  value. This prevents from assigning one to a variable, or to combine then with another statement.
- Finally, it's not deterministic and depends on the context of the program. If these statements were about saving
  some data in a database, we would depend on its availability, and the quality of the network. Same thing if the
  function requires some global state to be initialized before being called.
- The main problem is that statements are context dependent. The caller needs to know about the context of a statement 
before executing it, preventing it from being used in multiple arbitrary contexts. Unlike values, it is impossible to 
reuse them outside of the context they were defined for, which is the main fundamental of abstraction and 
software design. that is the ability to obtain complex components by combining simpler ones.

---
## Statements VS Values
```java
// Hypothetical code
??? p1 = System.out.println("Please enter ");
??? p2 = System.out.println("your name.");
// would behave like System.out.println("Please enter your name.")
p1.combine(p2)
```

Note:
- For example, if I need to combine two `println`s, I would not be able to do it.

---
## Statements As Values

```java
interface Console {
  static Console END = new Console() {};
}
class PutStrLn implements Console { /* ... */ }
class GetStrLn implements Console { /* ... */ }

static Console putStrLn(String content, Supplier<Console> next) { 
  return new PutStrLn(content, next);
}

static Console getStrLn(Function<String, Console> next) { 
  return new GetStrLn(next);
}
// more constructors...
// ...
```
- Statements have to be brought back to the world of values
- Each statement is converted to a value used to model what the program should do

Note:
- So what can we do about this? First of all, we need to convert these statements into values.

---

---
## Appendix: Referential Transparency

- To be referentially transparent, a function has to be:
  - <span style="color:#88B8F7">**Total**</span>: so it maps every input to an output<br/>
  - <span style="color:#88B8F7">**Pure**</span>: so its only effect is the computation of its returning value<br/>
  - <span style="color:#88B8F7">**Deterministic**</span>: so it returns the same output given the same input<br/>

---
## Appendix: Inheritance VS Composition

- Inheritance:
  - is about hierarchical classification
  - makes a sub-class tightly coupled to its parent class
  - should be used to add/extend features only

- Composition:
  - is closer to how we model real-world domains
  - keeps components loosely coupled (Open-Closed principle)
  - can be used to add/remove features

Note:
- Another important aspect is the relationship between Inheritance and Composition. Inheritance is about hierarchical classification. It arranges concepts from generalized to specialized, grouping related concepts in subtrees and so on.
- The semantics of a class is captured by its interface, that is the set of messages it can send and respond to. A sub-class must hold the inherent contract of its parent, that is any invariants, pre-conditions and post-conditions, as explained by the Liskov principle. This makes the sub-class tightly coupled to its superclass.
- As a consequence, one cannot modify a feature from a super class without affecting all its children.
- Composition on the other hand is closer to how we model real-world domains. We tend to think more in terms of parts and components than in terms of classification.
- Secondly, it aligns with the open close principle which state that a class should be extendable without modifying its definition.
- Easier to extend features.
- https://www.thoughtworks.com/insights/blog/composition-vs-inheritance-how-choose
- In a nutshell, inheritance is not composition. Inheritance has its own set of use-cases but in this talk we focus on composition.

---
## Statements are contextual

```java
// May throw an ArithmeticException
public User findUserById(UserId userId) {
  // if user is not valid
  if(isUserIdNotValid(userId)) {
    throw new InvalidUserException();
  } else if(UserId(42).equals(userId)) {
    return null;
  } else {
    User user = /* ... */
    return user;
  }
}
```
- This function requires:
 - a null-check from the caller
 - some error handling logic
- The function is not defined for every input
- It is not total.

---
## Statements are contextual

```java
UserRepo globalRepo = /* ... */


public String getUser(UserId userId) {
  return globalRepo.findById(userId);
}
```
- This function requires some state to be initialized
- It is therefore not pure

---

## Statements VS Values

In order to become 
<b>Total</b>: that is every input has to be mapped with an output<br/>
<b>Pure</b>: A function's only effect is the computation of its returning value<br/>
<b>Deterministic</b>: Given the same input, a function has to always return the same output<br/>

---
## Referential Transparency

```java
int i = 42;
int j = i + 1; // i could be replaced by 42
System.out.println(j);
```
```java
int increment(int i) {
  return i + 1;
}
// increment(2) could be replaced by 3
int j = increment(2);
System.out.println(j);
```
- An expression is referentially transparent if it can be replaced by the value it produces.

Note:
- An expression can be a variable or a function call.
- An expression is referentially transparent if it can be replaced by the value it produces without affecting the program's output in any way.

---
## 

<b>Total</b>: that is every input has to be mapped with an output<br/>
<b>Pure</b>: A function's only effect is the computation of its returning value<br/>
<b>Deterministic</b>: Given the same input, a function has to always return the same output<br/>

Note:
- https://ptolemy.berkeley.edu/projects/cps/Modularity_and_Composability.html
- 

- An interesting effect of referential transparency is that it makes your code  context-independent, meaning that an expression can be run in any order or in any context and it will always return the same result. This feature allows you to safely substitute a pure function with a different implementation depending on the context.

- context independent
- order of evaluation
- side-effects
- immutability

Our ability to decompose a problem into parts depends directly on our ability to glue solutions together - John Hughes

---
## Composability 

---
## Composability 
 - is about relationships between components
 - aims to produce highly modular and flexible systems
 - measures how coupled is a component to one another
 - also referred to as Modularity

Note:
- There can be many different relationships between components in a system. However, they all share one common aspect which is the level of dependency they introduce.
- What do we mean by dependency? It's a measure indicating how coupled a component is to another. The more coupled a component is to another, the more dependent it is.
- This measure is also referred to as modularity, and it's not wonder that modularity and composability are used interchangeably.
- This also means that the factors responsible for limiting these are also pretty similar, and in this case it's anything that increases the level of dependency between components.

---
## Dependency

- Anything limiting the reuse of a component in an arbitrary context
- Any assumptions/requirements made by the component

Note:
- The more is needed to use a component, the less you can reuse it in multiple context
- The problem is that one cannot eliminate all the requirements or the assumptions of a function. You have to start from somewhere.
- In other words, dependencies can be minimized and mitigated but cannot be eliminated.
- In the next few slides, we'll look at some examples fo dependencies and then provide some techniques showing how to manage dependencies properly

---
## Dependency: Environment

- Component is aware of the environment it lives in
- Symptoms:
 - hard to test
 - hard (if not impossible) to reuse
 - must live in the target environment (prod)

Note:
- Most obvious dependency, this is usually what people think about when thinking about a dependency
- Component has to be aware about the environment it lives/is deployed in

---
```java
class UserService {
  // ...
  public void save(User user) throws Exception {
    // Some validation business logic
    if(isUserValid(user)) {
      PreparedStatement preparedStmt = 
        conn.prepareStatement(
          "INSERT INTO USER(name, age) VALUES (?, ?)"
        );
      preparedStmt.setString(1, user.getName());
      preparedStmt.setInt(1, user.getAge());

      preparedStmt.execute();
      // ...
    }
  }
}
```
---
## Dependency: Temporal dependency/coupling

- Instruction must be done in a specific order
- Switching instructions affects the output or crash the program
- Symptoms:
  - The program generates invalid state at runtime
  - The program's output is non-deterministic

Note:
- Also known as Output dependency and Anti-dependency
- Usually happens when you rely on some hidden state (sous-jacent)
- NullPointerException
- Typical of imperative style

---
```java
class UserSession {
  private User user;

  public void setUser(User user) {
      this.user = user;
  }

  public String getUserName() {
    return user.getName();
  }
}
```
```java
UserSession session = new UserSession();

// Switching these instructions will
// throw an NullPointerException.
session.setUser(new User());
session.getUserName();
```
---
```java
class UserSession {
  // ...
  public void setUserName(String name) {
    user.setName(name);
  }
}
```
```java
UserService service = new UserService();
// Thread 1
service.setUserName("John");

// Thread 2
service.setUserName("Paul");

// Thread 3
// The output of this function is non-deterministic
service.getUserName();
```

Note:
- https://www.pluralsight.com/tech-blog/forms-of-temporal-coupling/

---
## Dependency: Control Dependency
- Anything required to run an instruction besides calling it
- Symptoms:
  - null-checks
  - try-catches
  - ...

Note:
- Any kind of ceremony that must be done in order to call a function
- If it's not done, the program may have unexpected outputs

---
```java
public String getUserName(long userId) {
  User user = userService.findUserById(userId);
  // Control dependency
  if(user == null) {
    // Another control dependency: We assume this is handled
    throw new UserNotFoundException(userId);
  } else {
    return user.getName();
  }
}
```
---
## Dependency: Global state

- Any state that is shared across multiple components
- Symptoms:
  - State corruption
  - Synchronization

---
```java
public String getData(long id) {
  return DataRegistry.getInstance().findById(id);
}
```
---
## Dependency

- Overall, dependencies can be categorized in terms of:
 - inputs: Anything required by the function to run
 - outputs: Anything generated by the function once run
- Moreover, these two can be either explicit or implicit

Note:
- input: Any dependency that is provided to the function called (eg. environment, shared state, ...)
- output: Any dependency that is generated by the function called (eg. control/temporal ...)

---
## Input dependencies

```java
public String getData(long id) {
  // The singleton instance is an implicit input dependency
  return DataRegistry.getInstance().findById(id);
}
```
```java
class UserSession {
  private User user;
  // ...
  public String getUserName() {
    // The user instance is an implicit input dependency of
    // this function
    return user.getName();
  }
}

UserSession session = new UserSession();
// This function requires a User instance to be set
// in order to be called to be called
session.getUserName();
```
---
## Output dependencies
```java
class UserSession {
  private User user;
  // ...
  public void setUserName(String name) {
    // The function generates an implicit output
    // not captured by its return type
    user.setName(name);
  }
}
```
```java
public String getUserName(long userId) {
  User user = userService.findUserById(userId);
  if(user == null) {
    // The function can generate an exception which 
    // is an implicit output not captured by its return
    // type
    throw new UserNotFoundException(userId);
  } else {
    return user.getName();
  }
}
```

Note:
- Are less obvious but are still there

---
- Explicit dependencies are easier to reason about

---
```java

```


---

Examples with a function input/output
Examples with a Singleton
Examples with a value passed through a constructor and used in a function
Talk a bit about Spring (???)
Provide examples when dependencies are explicit

---
## Make dependencies explicit

Explain Referential Transparency without mentioning it
Go with pure functions and immutable data-structures

---
## How does this help?


 Talk about composable APIs
 Mention people who complain about the language when the problem is about how they use it 

---

Start with SOLID, explain each principle briefly

Liskov: Contract, hard to achieve, composition over inheritance 
Interface segregation: SRP to the max along with inversion of control and dependency injection

---

Goal: Mitigate the dependency level from one layer to another.
Goal: Push the dependencies that do not change a lot to the edges of the architecture

---

Hexagonal Architecture

- Domain boundaries (mostly deals with coupling)

---

Other forms of dependencies?

---
## Execution / Imperative

```java
long userId    = saveUser(user);
long addressId = saveUserAddress(userAddress);

return new UserRow(userId, addressId, user, userAddress)
```
- execution dependency (blocking)

---

---
```java
UserService service = new UserService();
// Thread 1
service.setUser(user);

// Thread 2
service.getUserName();
```

- ...or things are not loaded in the order you expect (temporal dependencies)
- cannot be achieved without blocking

---

```java
class UserService {
  // ...
  public synchronized String getUserName() {
    if(user == null)
      return user.getName();
    else
      return null;
  }
}
```

- Leads to awkwardness
- Hard to reason about
- Hard to maintain
- Potential bugs.
- Blocking

=> To prevent bugs and ease the reading, we need to manage dependencies.

---

- Establish proper boundaries (mutation boundaries, asynchronous boundaries, ...)
- Make dependencies explicit

Note:
- Need to know about what a function really does, really requires, really outputs
- Provide guarantees about what each function does

---
Example:
- Inversion of control
- Problem, we always required to inject the dependency (we may not have it at this point)
- So let's make this transactional (it takes a function just like a Reader)
- Separation of coupling between the declaration and execution
- we can now compose.

- what about recursive calls? may explode the stack. We can rely on moving from the stack to the heap. Use a lazy valued structure (like a Stream)
- hard to debug if too many nested functions

How to design composable apis:
- operators, primitives, constructors 

---

Boundaries are exposed through APIs, these apis tell the caller using a protocol of communication.

---

Transactional context



---
Temporal coupling: Sequencing, Waiting, 
Data dependencies (Flow dependency vs Anti-dependency, Control Dependency (if else), Output dependency, https://en.wikipedia.org/wiki/Data_dependency)


----

non-deterministic order of execution
null-checks are data dependency (control dependency in fact)

Another aspect is designing using control-flow instead of data flow
making these dependencies explicit is the key
checked exceptions are better than nothing but are not composable

mutation boundaries
asynchronous boundaries
safe/unsafe boundaries

interface and boundaries

Proper dependency management leads to composable code
Predicate is a good example of a component DSL

---
## Dependency

```java
int length(List<Int> is) { /* ... */ }
```
```java
int <A> length(List<A> as) { /* ... */ }
```

Note:
- It would be interesting to show how using generic types can restrain the dependencies



https://www.thoughtworks.com/insights/blog/composition-vs-inheritance-how-choose
https://blog.statebox.org/modularity-vs-compositionality-a-history-of-misunderstandings-be0150033568


Talk about statements / custom operators and custom control flow operator

```java
class Println {
  public final String content;
  public final Supplier<Console> next;

  public Println(String content, Supplier<Console> next) {

  }
}

public Println repeat(String s, int n) {
  if(n <= 1) {
    return new Println(s);
  } else {
    return new Println(s, () => repeat(s, n - 1));
  }
}


```

statements have coupled context, while values don't. opens the doors for optimizations, and the ability to consider
an instruction from a more global perspective. 


---
## Composition patterns

- There are two main composition patterns:
  - product composition
  - sum composition
- Let's look at an example