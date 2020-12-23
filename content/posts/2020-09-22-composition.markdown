---
layout: post
title:  Composition
date:   2020-09-22 08:51:48 -0500
brief:
---

**An efficient software design is one allowing its components to be separated and recombined without introducing unexpected behaviors**. This topic has been tackled over and over in the past and different approaches like the [SOLID principles](https://en.wikipedia.org/wiki/SOLID) or the [GOF patterns](https://en.wikipedia.org/wiki/Design_Patterns) eventually came up to address this problem. Despite their value, these tend to confuse many software developers however. Taken separately, they may indeed sound incomplete and often fail to convey what ties them all together.

If we look at this from a higher perspective, they all share the same goal: **Making it easier to introduce new business requirements and modify existing ones, while preserving certain properties**. In other words: **Composition**. In this post, we'll look at some simple composition techniques along with some of the perspectives they offer in terms of design.

## Papers, please

> [Papers, please](https://en.wikipedia.org/wiki/Papers,_Please) is a game created by Lucas Pope in which the player takes on the role of a border-crossing immigration officer in a fictional dystopian country. The game takes place at a migration checkpoint. As the immigration officer, the player must review each immigrant and return citizen's passports and other supporting paperwork against an ever-growing list of rules using a number of tools and guides, allowing in only those with the proper paperwork while rejecting those without all proper forms, and at times detaining those with falsified information, while also balancing personal finances. 

In the next sections, we'll model a simplified version of this game to illustrate different patterns you may come across while writing software.

## The domain

[Papers, please](https://en.wikipedia.org/wiki/Papers,_Please)'s domain is pretty simple in essence and can be thought about like a business rule engine where each rule defines whether a person can be let through the border or not. First, we need to model the different documents that could be required by the immigration office:

```scala
type Date = Long
type UID  = String

/* (firstName, lastName, Date Of Birth) */
type Id = (String, String, Date)
sealed trait Document
object Document {
  /*
   * Required by any person attempting to cross the border except 
   * in the case of a asylum request.
   */
  final case class Passport(
    uid: UID,         // A unique id tying a person's papers
    id: Id,           // The owner's identity
    expiration: Date, // the passport's expiration date
    foreign: Boolean  // tells if the passport is foreign or not
  ) extends Document

  /* Required by all foreigners */
  final case class EntryPermit(uid: UID, id: Id, expiration: Date) 
    extends Document

  /* Required by citizens getting back in the country */
  final case class IdCard(uid: UID, id: Id) extends Document

  /* Required by asylum seekers */
  final case class FingerPrints(data: String) extends Document

  /* Required by asylum seekers */
  final case class GrantOfAsylum(
    uid: UID,
    id: Id,
    fingerPrints: FingerPrints
  ) extends Document
}
```
A `Rule` defines in what circumstances a person is allowed to cross the border. It's essentially a function taking some input and returning a `Result`.
```scala
case class Rule[A](run: A => Result)
```
A `Result` states whether a person can cross the border or not (`Approved` or `Denied`). If it comes out that some papers have been forged, their owner ends up in custody (`Detained`). Finally, the game defines that the checking process can be aborted in case of extreme circumstances like a terrorist attack (`Aborted`).
```scala
sealed trait Result
object Result {
  case object Approved extends Result // Visitor can be let through
  case object Denied   extends Result // Requirements are not met
  case object Detained extends Result // Papers are forged
  case object Aborted  extends Result // In case of a terrorist attack
}
```
## The rules

Having this in mind, let's try to model some rules:
```scala
object Rule {

  /* Creates a `Rule` from a boolean function */
  def isTrue[A](f: A => Boolean): Rule[A] =
    Rule(a => if (f(a)) Approved else Denied)

  /* Succeeds if the passport is not expired */
  val notExpired: Rule[(Date, Date)] = isTrue {
    case (left, right) => left <= right
  }

  /* Succeeds if the passport belongs to a citizen */
  val citizenPassport: Rule[Passport] = isTrue(p => !p.foreign)

  /* Succeeds if the passport belongs to a foreigner */
  val foreignPassport: Rule[Passport] = isTrue(_.foreign)

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
}
```
In [Papers, please](https://en.wikipedia.org/wiki/Papers,_Please), the border can only be crossed if the visitor provides:
- a non-expired citizen passport and a matching id card
- a non-expired foreign passport and a matching entry permit
- a grant of asylum and matching fingerprints
In any other case, the visitor either provided the wrong documents or attempted to cross the border with forged papers.

The above rules provide solutions for the most basic cases, but cannot cover more complex ones like we just described.

## Composing rules

Ideally we'd like to avoid duplication and make these rules composable. Let's try to model the citizen rule which requires a non-expired citizen passport along with a matching id-card:
```scala
object Rule {
  // ...
  val citizen: Rule[(Date, Passport, IdCard)] = Rule {
    case (now, passport, id) =>
      val result0 = citizenPassport.run(passport)
      val result1 = notExpired.run((passport.expiration, now))
      val result2 = passportMatchesIdCard.run((passport, id))
      // No way to combine results so far
      ???
  }
}
```
We currently have no way to combine results, and therefore need to add some machinery to achieve that. Let's add an operator `&&` to `Result`:
```scala
sealed trait Result { self =>
  def &&(that: Result): Result =
    (self, that) match {
      case (_, Aborted)  => that // process has been aborted
      case (Approved, _) => that // left side is ok, keep proceeding
      case _             => self // left side is not ok, do not proceed
    }
}
```
The citizen rule can be now built like this:
```scala
object Rule {
  // ...
  val citizen: Rule[(Date, Passport, IdCard)] = Rule { ctx =>
    case (now, passport, id) =>
      val result0 = citizenPassport.run(passport)
      val result1 = notExpired.run((passport.expiration, now))
      val result2 = passportMatchesIdCard.run((passport, id))
      result0 && result1 && result2
  }
}
```
This leads us to a first best practice to make a DSL composable: **provide binary operators returning their inputs type**:
```scala
(A, A) => A
```
This is exactly what we've done with `Result.&&` which takes two `Result`'s (`self` and `that`) and returns another `Result` combining them. This operator  helped us implementing the `citizen` but we can do better. To simplify the addition of other rules, we should also provide a combinator to `Rule` so that:
```scala
// Pseudo code:
val citizen = 
  citizenPassport && passportNotExpired && passportMatchesIdCard
```
Let's build this step by step. First we need to add the operator in question:
```scala
case class Rule[A](run: A => Result) { self =>
  /* 
   * Combines two rules and returns another one requiring a product 
   * of their input
   */
  def &&[B](that: Rule[B]): Rule[(A, B)] = Rule {
    case (a, b) => self.run(a) && that.run(b)
  }
}
```
The citizen rule can now be formed like following:
```scala
val citizen: Rule[((Passport, (Date, Date)), (Passport, IdCard))] = 
  citizenPassport && notExpired && passportMatchesIdCard
```
`citizen`'s type is awkward however and contains many redundancies. Ideally, we'd like something closer to:
```scala
val citizen: Rule[(Passport, Date, IdCard)] = ???
```
In order to run this rule, we would have to provide a tuple containing the passport, the current date, and a matching id card. These inputs could be then redistributed to the different underlying rules forming the resulting composition. This operator would therefore look like this:
```scala
def bothWith[B, C](that: Rule[B])(f: C => (A, B)): Rule[C] = ???
```
In other words, as long as we know how to decompose an input `C` into a product of `A` and `B`, we can combine two rules taking respectively an `A` and a `B`. The implementation happens to be quite straightforward:
```scala
case class Rule[-A](run: A => Result) { self =>
  // ...
  /*
   * Combines two rules respectively requiring an `A` and a `B`
   * into a rule requiring a `(A, B)`.
   */
  def bothWith[B, C](
    that: Rule[B]
  )(f: C => (A, B)): Rule[C] = Rule { c =>
    val (a, b) = f(c)
    self.run(a) && that.run(b)
  }
}
```
As a matter of fact, `&&` can be expressed in terms of `bothWith` making it a **derived operator**:
```scala
case class Rule[A](run: A => Result) { self =>
  /*
   * Alias for `both`
   */
  def &&[B](that: Rule[B]): Rule[(A, B)] =
    both(that)
  
  /* Alias for `bothWith` provided with the `identity` function */
  def both[B](that: Rule[B]): Rule[(A, B)] =
    bothWith(that)(identity)
  
  /*
   * Combines two rules respectively requiring an `A` and a `B`
   * into a rule requiring a product of A and B.
   */
  def bothWith[B, C](
    that: Rule[B]
  )(f: C => (A, B)): Rule[C] = Rule { c =>
    // As long as we know how to extract `A` et `B` from `C`, 
    // we can build a `Rule[C]` from a `Rule[A]` and a `Rule[B]`
    val (a, b) = f(c)
    self.run(a) && that.run(b)
  }
}
```

`bothWith` is one of the typical composition patterns you may come across while writing composable software. It fits well with data-structures being [**invariant** or **contravariant**](https://contramap.dev/2020/02/12/variance.html) in `A`, that is which provide functions taking/consuming `A`'s. `bothWith`  happens to be the solution required to model the citizen rule:
```scala
/*
 * A citizen must provide a non-expired passport along with a matching
 * id card
 */
val citizen: Rule[(Date, Passport, IdCard)] =
  (citizenPassport && notExpired)      // Rule[(Passport, (Date, Date))]
    .bothWith(passportMatchesIdCard) { // Rule[(Passport, IdCard)]
      case (now, passport, idCard) =>
        ((passport, (passport.expiration, now)), (passport, idCard))
      //     ^                    ^                      ^
      // citizenPassport     notExpired          passportMatchesIdCard
    }
```
We first combine `citizenPassport` and `notExpired` into a `Rule[(Passport, (Date, Date))]`, then call `bothWith` to combine the result into a `Rule[(Date, Passport, IdCard)]` using a function taking the provided input and returning a tuple containing each underlying rule's input. We can proceed the same way to define the foreigner rule (We decomposed the process into more steps for learning purpose):
```scala
/*
 * A foreigner must provide a non-expired passport and a matching
 * entry permit
 */
val foreigner: Rule[(Date, Passport, EntryPermit)] = {
  val step1: Rule[(Passport, (Date, Date))] = 
    foreignPassport && notExpired

  val step2: Rule[(Passport, EntryPermit)]  = 
    passportMatchesEntryPermit

  step1.bothWith(step2) {
    case (now, passport, permit) =>
      ((passport, (passport.expiration, now)), (passport, permit))
    //     ^                   ^                        ^
    // foreignPassport     notExpired       passportMatchesEntryPermit
  }
}
```
We now have almost all the pieces to define the general rule of the game. To do so, we have to compose the `citizen` with the `foreigner` so that:
```scala
// Pseudo code
val visitorRule: Rule[???] = citizen || foreigner
```
We don't have any operator for doing such composition yet, but before coding anything let's ask ourselves what the return type of that operator should be. If we think about it, the resulting type should capture the idea that the rule requires one **or** another input. For this reason, `Either` is a natural choice:
```scala
type Citizen   = (Date, Passport, IdCard)
type Foreigner = (Date, Passport, EntryPermit)
type ||[A, B]  = Either[A, B]

val visitorRule: Rule[Citizen || Foreigner] = ???
```
Let's now implement the operator needed to do such composition:
```scala
case class Rule[A](run: A => Result) { self =>
  // ...
  def ||[B](that: Rule[B]): Rule[Either[A, B]] =
    Rule { 
      case Left(a)  => self.run(a)
      case Right(b) => that.run(b)
    }
}
```
The implementation is pretty straightforward, but the result is not that great:
```scala
type Citizen   = (Date, Passport, IdCard)
type Foreigner = (Date, Passport, EntryPermit)
type ||[A, B]  = Either[A, B]

val visitorRule: Rule[Citizen || Foreigner] = citizen || foreigner
/* which is equivalent to
val visitorRule: Rule[
  (Date, Passport, IdCard) || 
  (Date, Passport, EntryPermit)
] = citizen || foreigner
*/
```
Just like earlier we have too many redundancies in the resulting type and some refinement is required. Let's implement a more generic version of `||`:
```scala
case class Rule[A](run: A => Result) { self =>
  /*
   * Combines two rules respectively requiring an `A` and a `B`
   * into a rule requiring either an `A` or a `B`.
   */
  def eitherWith[B, C](
    that: Rule[B]
  )(f: C => Either[A, B]): Rule[C] =
    Rule { c =>
      f(c) match {
        case Left(a)  => self.run(a)
        case Right(b) => that.run(b)
      }
    }
}
```
In other words, as long as we know how to convert an input `C` into an either of `A` and `B`, we can combine two rules taking respectively an `A` and a `B`. Just like `bothWith`, `eitherWith` happens to be a primary operator from which `||` is derived:
```scala
case class Rule[-A](run: A => Result) { self =>
  /*
   * Alias for `either`
   */
  def ||[B](that: Rule[B]): Rule[Either[A, B]] =
    either(that)

  /*
   * Alias for `eitherWith` with the identity function provided
   */
  def either[B](that: Rule[B]): Rule[Either[A, B]] =
    eitherWith(that)(identity)
}
```

`eitherWith` is another typical composition pattern you may come across while writing composable software. It fits well with data-structures being [**invariant** or **covariant**](https://contramap.dev/2020/02/12/variance.html) in `A`, that is which provide functions returning/producing `A`'s. This new operator happens to be exactly what we need to compose the visitor rule:
```scala
/*
 * A visitor other than a refugee must either:
 * - provide a valid passport and an id card (citizen rule)
 * - or provide a valid passport and an entry permit (foreigner rule)
 */
val visitor: Rule[(Date, Passport, IdCard || EntryPermit)] =
  citizen.eitherWith(foreigner) {
    case (now, passport, Left(idCard))  => Left((now, passport, idCard))
    case (now, passport, Right(permit)) => Right((now, passport, permit))
  }
```

## Sum and Product composition

You may notice a certain similarity between `bothWith` and `eitherWith`. This is not surprising, as they both define two kinds of composition patterns respectively known as **product composition** and **sum composition**:
```scala
def bothWith[B, C]  (that: Rule[B])(f: C => (A, B)): Rule[C] = /* ... */

def eitherWith[B, C](that: Rule[B])(f: C => A || B): Rule[C] = /* ... */
```
These two patterns mirror each others in regards to composition, and it's overall a best practice to look for them whenever you write a composable DSL.

## Fallback rule

In some cases, the game may be aborted under extreme circumstances such as a terrorist attack. Let's see how we could reflect that in our model:
```scala
// Pseudo code
val game: Rule[(Passport, IdCard || EntryPermit)] =
  visitor + terroristAttack
```
First let's think about how `terroristAttack` should be represented. Overall, this rule does not care about the document provided. So technically it could take any document, and as an output always return `Aborted`:
```scala
Rule(_ => Aborted)
```
Secondly, combining `terroristAttack` with another rule should not change the type of the latter. So the combination of `terroristAttack` with `visitorRule` should have the same type than `visitorRule`. This can be implemented in different ways, using **contravariance** for example:
```scala
// Note the - in front of A
case class Rule[-A](run: A => Result) { /* ... */ }
val terroristAttack: Rule[Any] = Rule(_ => Aborted)
```
Let's now encode the operator combining `visitor` with `terroristAttack`:
```scala
case class Rule[-A](run: A => Result) { self =>
  def orElse[A0 <: A](that: Rule[A0]): Rule[A0] =
    Rule { a =>
      val r0 = self.run(a)
      val r1 = that.run(a)
      ???
    }
}
```
To finish this implementation, we'll need to provide a way to combine `Result` so that `r0` falls back to `r1` whenever it's not `Approved`:
```scala
sealed trait Result { self =>
  // ...
  def ||(that: Result): Result =
    self match {
      case Detained | Denied => that
      case _                 => self
    }
}
```
Using this new combinator, we can now finish `orElse`'s implementation:
```scala
case class Rule[-A](run: A => Result) { self =>
  // ...
  def orElse[A0 <: A](that: Rule[A0]): Rule[A0] =
    Rule { a =>
      val r0 = self.run(a)
      val r1 = that.run(a)
      r0 || r1
    }
}
```
As you can guess, this is another typical composition pattern that can be generalized like following:
```scala
case class Rule[-A](run: Context[A] => Result) { self =>
  // ...
  def orElse[A0 <: A](that: Rule[A0]): Rule[A0] =
    zipWith(that)(_ || _)

  def zipWith[A0 <: A](
    that: Rule[A0]
  )(f: (Result, Result) => Result): Rule[A0] =
    Rule { ctx =>
      val r0 = self.run(ctx)
      val r1 = that.run(ctx)
      f(r0, r1)
    }
}
```
This implementation of `zipWith` is specific to our domain. Generally, `zipWith` looks more like the following:
```scala
case class Data[A](value: A) { self =>
  // ...
  def zip[A, B](that: Data[B]): Data[(A, B)] =
    zipWith(that)((_, _))

  def zipWith[B, C](
    that: Data[B]
  )(f: (A, B) => C): Data[C] =
    Data { ctx =>
      f(self.value, that.value)
    }
}
```
In our example however, having a `Rule` returning a product of `Result` (that is a `(Result, Result)`) is unsound from a domain perspective. `zipWith` is a **covariant analogue** of `bothWith`. It is mostly found in invariant and covariant data-structures and is probably one of the most common composition pattern. With this new tool in our belt, we can finish the description of the game's rules:
```scala
val game: Rule[(Passport, IdCard || EntryPermit)] =
  visitor.orElse(terroristAttack)
```
Hold on, what about the refugee rule? Thanks to our new combinators, introducing that one is a piece of cake:
```scala
type Refugee = (GrantOfAsylum, FingerPrints)
type Visitor = (Date, Passport, IdCard || EntryPermit)

val game: Rule[Visitor || Refugee] =
  (visitor || refugee).orElse(terroristAttack)
```

## Going further

Is that it? Well, actually there's more we can talk about regarding this topic. So far we represented every terms of our DSL using a simple function:
```scala
A => Result
```
In other words, the evaluation of the solution is embedded in the resulting data-structure. This happened to be pretty useful but there are some drawbacks:
- **Testability**: the solution being a function, there is no way to inspect its content without executing it. If the function has unexpected side-effects, we would lose the ability to reason about it locally.
- **Optimization**: rules can currently only be composed using function composition. This prevents us to perform any optimization in the way these functions are called and run.
- **Extendability**: if we had to provide another way to evaluate the solution, we would have to change every operator (`bothWith`, `eitherWith`, `zipWith`) and primitive (`notExpired`, `refugee`, `passportMatchesIdCard`, ...) provided by this DSL.

Another way to encode this domain would be to solely rely on pure data-structures. Instead of embedding the evaluation function, we can put that one aside and represent the output of each operator using a date-structure:
```scala
object DSL {
  type ||[A, B] = Either[A, B]

  final case class Always[F[_]](result: Result) extends DSL[Any, F]

  final case class OrElse[A, F[_]](
    left: DSL[A, F], 
    right: DSL[A, F]
  ) extends   SL[A, F]

  final case class BothWith[A, B, C, F[_]](
    left: DSL[A, F],
    right: DSL[B, F],
    f: C => (A, B)
  ) extends DSL[C, F]

  final case class EitherWith[A, B, C, F[_]](
    left: DSL[A, F],
    right: DSL[B, F],
    f: C => A || B
  ) extends DSL[C, F]

  case class Pure[F[_], A](fa: F[A]) extends DSL[A, F]
}
```
But what is that `F[_]`? `F[_]` is a way to compose this dsl with another one, like `Rule` for example:
```scala
sealed trait Rule[-A]
object Rule {
  final case object CitizenPassport extends Rule[Passport]
  final case object ForeignPassport extends Rule[Passport]

  final case object Refugee
    extends Rule[(GrantOfAsylum, FingerPrints)]

  final case object PassportMatchesIdCard 
    extends Rule[(Passport, IdCard)]

  final case object PassportMatchesEntryPermit 
    extends Rule[(Passport, EntryPermit)]

  final case object NotExpired      extends Rule[(Date, Date)]
  final case object TerroristAttack extends Rule[Any]
}
```
The combination of these two DSLs would look like this:
```scala
type RuleF[A] = DSL[A, Rule]

val notExpired: RuleF[(Date, Date)]  = Pure(NotExpired)
val citizenPassport: RuleF[Passport] = Pure(CitizenPassport)
val foreignPassport: RuleF[Passport] = Pure(ForeignPassport)
val terroristAttack: RuleF[Any]      = Pure(TerroristAttack)

val refugee: RuleF[(GrantOfAsylum, FingerPrints)] =
  Pure(Refugee)

val passportMatchesIdCard: RuleF[(Passport, IdCard)] =
  Pure(PassportMatchesIdCard)

val passportMatchesEntryPermit: RuleF[(Passport, EntryPermit)] =
  Pure(PassportMatchesEntryPermit)
```
What about the combinators? These actually do not need to change that much:
```scala
sealed trait DSL[-A, F[_]] { self =>
  def &&[B](that: DSL[B, F]): DSL[(A, B), F] =
    bothWith(that)(identity)

  def eitherWith[B, C](
    that: DSL[B, F]
  )(f: C => Either[A, B]): DSL[C, F] =
    EitherWith(self, that, f)

  def ||[B](that: DSL[B, F]): DSL[Either[A, B], F] =
    eitherWith(that)(identity)
  
  def bothWith[B, C](that: DSL[B, F])(f: C => (A, B)): DSL[C, F] =
    BothWith(self, that, f)
  
  def orElse[A0 <: A](that: DSL[A0, F]): DSL[A0, F] =
    OrElse(self, that)
}
```
and the remaining rules can be left as is, as they are expressed only using existing solutions (`refugee`, `citizenPassport`...) and operators (`&&`, `||`. `eitherWith`, ...):
```scala
val game: RuleF[Visitor || Refugee] =
  (visitor || refugee).orElse(terroristAttack)
```
Once we have the right data-structure, we need a function to evaluate it:
```scala
def run[A, F[_]](rule: DSL[A, Rule])(ctx: A): Result =
  rule match {
    case Always(r) => r

    case oe: OrElse[A, Rule] =>
      run(oe.left)(ctx) || run(oe.right)(ctx)

    case bw: BothWith[a, b, A, Rule] =>
      val (a, b) = bw.f(ctx)
      run(bw.left)(a) && run(bw.right)(b)

    case ew: EitherWith[a, b, A, Rule] =>
      ew.f(ctx) match {
        case Left(a)  => run(ew.left)(a)
        case Right(b) => run(ew.right)(b)
      }

    case Pure(fa) => eval(fa)(ctx)
  }

def eval[A](rule: Rule[A])(value: A): Result =
  rule match {
    case CitizenPassport => if (value.foreign) Denied else Approved
    case ForeignPassport => if (value.foreign) Approved else Denied

    case NotExpired =>
      val (l, r) = value
      if (l <= r) Approved else Denied

    case PassportMatchesIdCard =>
      val (passport, idCard) = value
      if (passport.uid == idCard.uid) Approved else Detained

    case PassportMatchesEntryPermit =>
      val (passport, permit) = value
      if (passport.uid == permit.uid) Approved else Detained

    case Refugee =>
      val (grant, prints) = value
      if (grant.fingerPrints.data == prints.data) Approved else Detained

    case TerroristAttack => Aborted
  }
```
Note that we use two functions here, one for each DSL. Secondly, these are not stack-safe and would crash if recursive rules are created. We can fix this using techniques such as Trampolining, but this will be the topic of another post.

## Taking some steps back

No matter the approach, we ended up using three types of block:
- **primitives**: which model simple solutions (`notExpired`, `CitizenPassport`...)
- **constructors**: which build solutions from existing solutions (`citizen`, `foreigner`, ...)
- **operators**: which transform/combine solutions into other solutions (`eitherWith`, `bothWith`, ...).

As explained by [John DeGoes](https://github.com/jdegoes) and [Ruurtjan Pul](https://ruurtjan.com/) in this [post](https://medium.com/bigdatarepublic/writing-functional-dsls-for-business-domains-1bccc5d3f62b), there are some best practices regarding how **primitives** should be designed:
>- Composable: to build complex solutions using simple components;<br/>
>- Orthogonal: such that there’s no overlap in capabilities between primitives;<br/>
>- Minimal: in terms of the number of primitives.<br/>

Secondly, in terms of encoding, we first embedded the evaluation function within the resulting data-structure. Later on we decided to pull it apart and use pure data-structures only. The first type of encoding is referred to as an **executable encoding**. It implies that _every constructor and operators of the model is expressed in terms of its execution_. It's defined in opposition with the **declarative encoding** used in the second implementation, _where every **constructor** and **operator** of the model is expressed as pure data in a recursive tree structure_.

Both types of encoding have their trade-offs. With an **executable encoding**, adding new constructors and operators is easy, as they are all defined using the same function, while adding new evaluation functions is harder, as it potentially requires changing every operator and constructors of the DSL. It's the exact opposite with a **declarative encoding** where adding new evaluation functions is easy (as these are defined separately), but adding new constructors and operators is painful. You may recognize the [expression problem](https://en.wikipedia.org/wiki/Expression_problem) here which is the very reason why Object Oriented Programming and Functional Programming exist.

Moreover, both encodings tend to perform better in specific cases. The **declarative encoding** will usually be a better fit for use-cases involving optimizations (thanks to inspections capabilities) and/or persistence (thanks to pure data-structures), while the **executable encoding** is a better choice when it comes to improve legacy code. If you would like to go further, it is highly suggested to read the [following post](https://degoes.net/articles/functional-design), which inspired this article a lot.

## Wrapping Up

This post was rather long, so here is a quick recap:
- To improve composition, define **primitives**, **constructors** and **operators** using the best practices described above
- Provide these with **binary operators** to compose them forever (`bothWith`, `eitherWith`, `zipWith`, ...)
- Use the adequate encoding (executable/declarative) depending on your use-case but keep in mind that a **declarative encoding** suits a greenfield project better.

The code of this post is all available [here](https://github.com/FrancisToth/francistoth.github.io/blob/master/src/main/scala/dev/contramap/composition/PapersPlease.scala). Thank you for reading, and thanks to [John De Goes for all his time teaching us writing better code](https://www.patreon.com/jdegoes/posts).

