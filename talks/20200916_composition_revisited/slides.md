## Composition

Note:
- Worked on a app generation engine
- Familiar with SOLID principles, but these are pretty vague
- end up over-applying them or not enough
- There must be something else, some clear practices describing how composable software design can be achieved
- This talk is about some of the things I've learnt since that time

---
## Why should we care?

- Writing Software:
 - is an iterative process
 - is about solving many small problems
 - combining intermediate solutions into one consistent delivery

Note:
- No one wakes up one day and decides to build a complex software
- Building software is an iterative process
- It's about solving many small problems which all together form a complex one.
- So more generally, Software Design is about...

---

**Combining small blocks into bigger ones**

Note:
- Or in more business friendly terms...

---

**Make it easy the introduction of new business requirements or the modification of existing ones**

---
## What does it imply?

- **Local Reasoning**: Ability to abstract over a component without knowing how it's implemented
- **Composition**: Ability to safely separate and recombine a system's components

Note:
- What does that imply concretely?
- If you cannot reason about a component without knowing about its internals, the bigger the component the harder it is to understand the system it is part of.
- Think about language in general. Our ability to build complex sentences relies on our capacity to label abstract concepts with a noun.
- For example, you don’t need to explain how a car works every time you want to talk about one.
- This is the fundamental principle of abstraction. Without it, nothing can be done
- Secondly, sustainable systems are first and foremost systems that can change over time, introduce new features and overall address problems for which they were not initially designed for.

---
## Papers, please
- Game created by Lucas Pope 
- Immigration officer simulation in a fictional dystopian country
- Described as an empathy game.

Note:
- In the following slides, we'll illustrate different best practices to write composable software, and to do so we'll use the domain of Papers, please.
- released in 2017 and received very positive feedback since
- It's described as an empathy game where there the player is pushed to make decisions quickly and decide whether a person can cross the border or not even if all the required papers are not provided.

---
## The domain

- Can be looked at as a business rule engine
- Each rule defines whether a person can be let through the border or not

Note:
"Papers, please"'s domain is pretty simple in essence and can be thought about like a business rule engine where each rule defines whether a visitor can be let through the border or not.

---
## The Rules

```scala
sealed trait Result
object Result {
  /* If the visitor can be let through */
  case object Approved extends Result
  /* If requirements are not met */ 
  case object Denied   extends Result
  /* If papers are forged */
  case object Detained extends Result
  /* In case of a terrorist attack */
  case object Aborted  extends Result
}

case class Rule[A](run: A => Result)
```
- A `Rule` defines the requirements for a visitor to cross the border.

---

## The Rules

- In [Papers, please](https://en.wikipedia.org/wiki/Papers,_Please), the border can only be crossed if the visitor provides:
  - a valid citizen passport and a matching id card
  - a valid foreign passport and a matching entry permit
  - a grant of asylum and matching fingerprints

---
## The Rules

```scala
/* Creates a `Rule` from a boolean function */
def isTrue[A](f: A => Boolean): Rule[A] =
  Rule(a => if (f(a)) Approved else Denied)

/* Succeeds if a document is not expired */
val notExpired: Rule[(Date, Date)] = isTrue {
  case (left, right) => left <= right
}

/* Succeeds if the passport belongs to a citizen */
val citizenPassport: Rule[Passport] = isTrue(p => !p.foreign)

/* Succeeds if the passport belongs to a foreigner */
val foreignPassport: Rule[Passport] = isTrue(_.foreign)
```
---
## The Rules

```scala
/* Succeeds if the passport matches the id card */
val passportMatchesIdCard: Rule[(Passport, IdCard)] = 
  isTrue { case (passport, idCard) => 
    passport.id == idCard.id && passport.uid == idCard.uid 
  }
  
/* Succeeds if the passport matches the entry permit */
val passportMatchesEntryPermit: Rule[(Passport, EntryPermit)] = 
  isTrue { case (passport, permit) => 
    passport.uid == permit.uid && passport.id == permit.id
  }
```
---
## Live coding

<img style="background-color: transparent; width: 5%" src="images/sweat-smiley.png"/><br/>

Note:
- Let's see how these rules could be combined

---
## Taking some steps back

- Binary operators everywhere!
- Two main composition patterns
 - sum-composition: `&&`, `either`, `eitherWith`
 - product-composition: `||`, `both`, `bothWith`, `zipWith`

---
## Taking some steps back

- [Papers, please](https://en.wikipedia.org/wiki/Papers,_Please) domain is modeled using:
 - **primitives**: which model solutions for _simple problems_
 - **operators**: which transform/combine existing solutions
 - **constructors**: which build solutions for _complex problems_

Note:
- As a matter of fact, many composable DSLs are modeled this way which is not surprising considering what we've seen. 

---
## Evaluation

```scala
run: A => Result
```

- The evaluation of the solution is embedded in the resulting data-structure
- Every constructor and operators is represented in terms of its execution (`A => Result`)
- This encoding is referred to as an **executable encoding**

---
## Taking some steps back

- It is defined in opposition to a **declarative encoding**
- A **declarative encoding** implies that _every **constructor** and **operator** of the model is expressed as pure data in a recursive tree structure_.

---
## Live coding

<img style="background-color: transparent; width: 5%" src="images/sweat-smiley.png"/><br/>

---
## Declarative vs Executable encoding

- Using an executable encoding:
  - adding new constructors and operators is easy
  - adding new interpreters however can be painful
- whereas in an declarative encoding
  - adding new interpreters is easy
  - adding new constructors and operators can be painful

---
## Declarative vs Executable encoding

- **Declarative encoding** tends to be better for use-cases involving optimizations and/or persistence
- **Executable encoding** is a better fit for legacy code

---
## Wrapping up

- A sustainable design:
  - requires **Local Reasoning** and **Composition**
  - is built on top of **primitives**, **constructors**, and **operators**
  - can be encoded using an **executable** or a **declarative** encoding
- Best practices:
  - **binary operator**
  - **sum/product composition patterns**
  - **pure function/data**
  - **Keep program's description and evaluation apart**
---
## References

- [An Introduction to Functional Design](https://degoes.net/articles/functional-design) by John DeGoes
- [Writing functional DSLs for business domains](https://medium.com/bigdatarepublic/writing-functional-dsls-for-business-domains-1bccc5d3f62b)

---
## Thank you! / Questions? 

Francis Toth - www.contramap.dev / www.yoppworks.com
