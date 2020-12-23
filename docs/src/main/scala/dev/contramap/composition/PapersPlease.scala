package dev.contramap.composition
import dev.contramap.composition.PapersPlease.Result.Approved
import dev.contramap.composition.PapersPlease.Result.Denied
import dev.contramap.composition.PapersPlease.Result.Detained
import dev.contramap.composition.PapersPlease.Result.Aborted

object PapersPlease {

  type Date = Long
  type UID  = String

  /* (firstName, lastName, Date Of Birth) */
  type Id = (String, String, Date)

  sealed trait Document
  object Document {
    /*
     * Required by any person attempting to cross the border except in the case of
     * a asylum request.
     */
    final case class Passport(
      uid: UID,
      id: Id,
      expiration: Date,
      foreign: Boolean
    ) extends Document

    /* Required by all foreigners */
    final case class EntryPermit(uid: UID, id: Id, expiration: Date) extends Document

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

  /* The result of a paper-check process */
  sealed trait Result { self =>
    import Result._

    def &&(that: Result): Result =
      (self, that) match {
        case (_, Aborted)  => that // the process has been aborted, do not proceed
        case (Approved, _) => that // the left side is ok, keep proceeding
        case _             => self // the left side is not ok, do not proceed
      }

    def ||(that: Result): Result =
      self match {
        case Detained | Denied => that // If left does not succeed, check the right
        case _                 => self // otherwise always return left
      }
  }
  object Result {
    case object Approved extends Result // If the visitor can be let through
    case object Denied   extends Result // If requirements are not met
    case object Detained extends Result // If papers are forged
    case object Aborted  extends Result // In case of a terrorist attack
  }

  import Result._
  import Document._

  case class Rule[-A](run: A => Result) { self =>
    /*
     * Alias for `both`
     */
    def &&[B](that: Rule[B]): Rule[(A, B)] =
      both(that)

    /* Alias for `bothWith` provided with the `identity` function */
    def both[B](that: Rule[B]): Rule[(A, B)] =
      bothWith(that)(identity)

    /*
     * Combines two rules respectively requiring an `A` and a `B` into a rule
     * requiring a `(A, B)`.
     */
    def bothWith[B, C](that: Rule[B])(f: C => (A, B)): Rule[C] = Rule { c =>
      // As long as we know how to extract `A` et `B` from `C`, we can build
      // a `Rule[C]` from a `Rule[A]` and a `Rule[B]`
      val (a, b) = f(c)
      self.run(a) && that.run(b)
    }

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

    /*
     * Combines two rules respectively requiring an `A` and a `B` into a rule
     * requiring either an `A` or a `B`.
     */
    def eitherWith[B, C](that: Rule[B])(f: C => Either[A, B]): Rule[C] =
      Rule { c =>
        f(c) match {
          case Left(a)  => self.run(a)
          case Right(b) => that.run(b)
        }
      }

    /*
     * Alias for `zip`
     */
    def orElse[A0 <: A](that: Rule[A0]): Rule[A0] =
      zipWith(that)(_ || _)

    /*
     * Alias for `zipWith` provided with the `_ && _`
     */
    def zip[A0 <: A](that: Rule[A0]): Rule[A0] =
      zipWith(that)(_ && _)

    /*
     * Creates a rule returning the combination of the outputs of two rules
     */
    def zipWith[A0 <: A](
      that: Rule[A0]
    )(f: (Result, Result) => Result): Rule[A0] =
      Rule { a0 =>
        val r0 = self.run(a0)
        val r1 = that.run(a0)
        f(r0, r1)
      }
  }

  object Rule {

    /*
     * -------------------------------------------------------------------
     * Primitives: Solutions for simple problems.
     * -------------------------------------------------------------------
     */

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

    /* Succeeds if a grant of asylum and matching fingerprints are provided */
    val refugee: Rule[(GrantOfAsylum, FingerPrints)] =
      isTrue { case (grant, prints) => grant.fingerPrints.data == prints.data }

    /* Succeeds if the passport matches the id card */
    val passportMatchesIdCard: Rule[(Passport, IdCard)] =
      isTrue { case (passport, idCard) => passport.id == idCard.id }

    /* Succeeds if the passport matches the entry permit */
    val passportMatchesEntryPermit: Rule[(Passport, EntryPermit)] =
      isTrue {
        case (passport, permit) =>
          passport.uid == permit.uid && passport.id == permit.id
      }

    /*
     * -------------------------------------------------------------------
     * Constructors: Solutions for complex problems.
     * -------------------------------------------------------------------
     */

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

    /*
     * A foreigner must provide a non-expired passport and a matching
     * entry permit
     */
    val foreigner: Rule[(Date, Passport, EntryPermit)] = {
      val step1: Rule[(Passport, (Date, Date))] = foreignPassport && notExpired
      val step2: Rule[(Passport, EntryPermit)]  = passportMatchesEntryPermit

      step1.bothWith(step2) {
        case (now, passport, permit) =>
          ((passport, (passport.expiration, now)), (passport, permit))
        //     ^                   ^                        ^
        // foreignPassport     notExpired       passportMatchesEntryPermit
      }
    }

    type ||[A, B] = Either[A, B]
    /*
     * A visitor other than a refugee must either
     * - provide a valid passport and an id card (citizen rule)
     * - or provide a valid passport and an entry permit (foreigner rule)
     */
    val visitor: Rule[(Date, Passport, IdCard || EntryPermit)] =
      citizen.eitherWith(foreigner) {
        case (now, passport, Left(idCard))  => Left((now, passport, idCard))
        case (now, passport, Right(permit)) => Right((now, passport, permit))
      }

    /*
     * In case of extreme circumstances, the whole game can be aborted.
     * - This happens whenever the checkpoint is under assault
     * - The additional rule should not change the resulting type
     */
    val terroristAttack: Rule[Any] = Rule(_ => Aborted)

    /*
     * Let's implement the final rule:
     * - it should work for a citizen, a foreigner, and a refugee
     * - it should abort the game if the checkpoint is under assault
     */
    type Refugee = (GrantOfAsylum, FingerPrints)
    type Visitor = (Date, Passport, IdCard || EntryPermit)

    val game: Rule[Visitor || Refugee] =
      (visitor || refugee).orElse(terroristAttack)
  }
}

object DeclarativeEncoding {
  import PapersPlease._
  import PapersPlease.Document._
  import PapersPlease.Result._
  import Rule._
  import DSL._

  sealed trait DSL[-A, F[_]] { self =>

    def &&[B](that: DSL[B, F]): DSL[(A, B), F] =
      bothWith(that)(identity)

    def ||[B](that: DSL[B, F]): DSL[Either[A, B], F] =
      eitherWith(that)(identity)

    def eitherWith[B, C](that: DSL[B, F])(f: C => Either[A, B]): DSL[C, F] =
      EitherWith(self, that, f)

    def bothWith[B, C](that: DSL[B, F])(f: C => (A, B)): DSL[C, F] =
      BothWith(self, that, f)

    def join[A0 <: A](that: DSL[A0, F]): DSL[A0, F] =
      bothWith(that)(a => (a, a))

    def orElse[A0 <: A](that: DSL[A0, F]): DSL[A0, F] =
      OrElse(self, that)
  }
  object DSL {
    type ||[A, B] = Either[A, B]

    /*
     * -------------------------------------------------------------------
     * Primitives: Solutions for simple problems. These should be:
     * - Composable: to build complex solutions using simple components
     * - Orthogonal: no overlap in capabilities between primitives
     * - Minimal: in terms of number
     * -------------------------------------------------------------------
     */
    final case class Always[F[_]](result: Result)                       extends DSL[Any, F]
    final case class OrElse[A, F[_]](left: DSL[A, F], right: DSL[A, F]) extends DSL[A, F]
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

  sealed trait Rule[-A]
  object Rule {
    // Papers, please DSL's primitives
    final case object CitizenPassport extends Rule[Passport]
    final case object ForeignPassport extends Rule[Passport]
    final case object Refugee         extends Rule[(GrantOfAsylum, FingerPrints)]

    final case object PassportMatchesIdCard      extends Rule[(Passport, IdCard)]
    final case object PassportMatchesEntryPermit extends Rule[(Passport, EntryPermit)]

    final case object NotExpired      extends Rule[(Date, Date)]
    final case object TerroristAttack extends Rule[Any]
  }

  type RuleF[A] = DSL[A, Rule]

  // Papers, please DSL's constructors
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

  /*
   * -------------------------------------------------------------------
   * Constructors: Solutions for complex problems.
   * These are built by combining existing solutions using operators
   * -------------------------------------------------------------------
   */
  val citizen: RuleF[(Date, Passport, IdCard)] =
    (citizenPassport && notExpired)
      .bothWith(passportMatchesIdCard) {
        case (now, passport, idCard) =>
          ((passport, (passport.expiration, now)), (passport, idCard))
      }

  val foreigner: RuleF[(Date, Passport, EntryPermit)] =
    (foreignPassport && notExpired)
      .bothWith(passportMatchesEntryPermit) {
        case (now, passport, permit) =>
          ((passport, (passport.expiration, now)), (passport, permit))
      }

  type ||[A, B] = Either[A, B]
  type Visitor  = (Date, Passport, IdCard || EntryPermit)
  type Refugee  = ((GrantOfAsylum, FingerPrints))

  val visitor: RuleF[Visitor] =
    citizen.eitherWith(foreigner) {
      case (now, passport, Left(idCard))  => Left((now, passport, idCard))
      case (now, passport, Right(permit)) => Right((now, passport, permit))
    }

  val game: RuleF[Visitor || Refugee] =
    (visitor || refugee).orElse(terroristAttack)

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
}
