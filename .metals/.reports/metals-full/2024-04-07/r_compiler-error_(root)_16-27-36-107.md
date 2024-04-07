file:///C:/Users/fchan/OneDrive%20-%20Efrei/M1/Semetre%208/Data%20Engineering/Project/Projet_DE/scala3/src/main/scala/Test.scala
### java.lang.AssertionError: assertion failed: denotation object language invalid in run 3. ValidFor: Period(4.1-2)

occurred in the presentation compiler.

presentation compiler configuration:


action parameters:
offset: 1083
uri: file:///C:/Users/fchan/OneDrive%20-%20Efrei/M1/Semetre%208/Data%20Engineering/Project/Projet_DE/scala3/src/main/scala/Test.scala
text:
```scala
import org.scalatest.funsuite.AnyFunSuite
import java.time.Instant

class Test extends AnyFunSuite {

  test("writeFileCSV should generate a CSV file named output.csv with ID, Timestamp and sentences") {
    val reports = List(
      IOTReport("ID001", Instant.parse("2024-04-01T12:00:00Z"), "Epita est meilleur que l'EFREI"),
      IOTReport("ID002", Instant.parse("2024-04-02T12:00:00Z"), "On en peut plus des frais qui augmentent")
    )

    val filePath = "output.csv"

    IOTReport.writeFileCSV(reports, filePath)

    val expectedContent =
      """ID_Student,Timestamp,Sentence
ID001,2024-04-01T12:00:00Z,Epita est meilleur que l'EFREI
ID002,2024-04-02T12:00:00Z,On en peut plus des frais qui augmentent""".stripMargin

    val actualContent = scala.io.Source.fromFile(filePath).mkString

    assert(actualContent === expectedContent)
  }

  test("writeFileJSON should generate a JSON file named output.json with with ID, Timestamp and sentences ") {
    val reports = List(
      IOTReport("ID001", Instant.parse("2024-04-01T12:@@00:00Z"), "Epita est meilleur que l'EFREI"),
      IOTReport("ID002", Instant.parse("2024-04-02T12:00:00Z"), "On en peut plus des frais qui augmentent")
    )

    val jsonFilePath = "output.json"

    IOTReport.writeFileJSON(reports, jsonFilePath)

    val expectedContent =
      """[
  {
    "ID_Student": "ID001",
    "Timestamp": "2024-04-01T12:00:00Z",
    "Sentence": "Epita est meilleur que l'EFREI"
  },
  {
    "ID_Student": "ID002",
    "Timestamp": "2024-04-02T12:00:00Z",
    "Sentence": "On en peut plus des frais qui augmentent"
  }
]""".stripMargin

    val actualContent = scala.io.Source.fromFile(jsonFilePath).mkString

    assert(actualContent === expectedContent)
  }
}

```



#### Error stacktrace:

```
scala.runtime.Scala3RunTime$.assertFailed(Scala3RunTime.scala:8)
	dotty.tools.dotc.core.Denotations$SingleDenotation.updateValidity(Denotations.scala:723)
	dotty.tools.dotc.core.Denotations$SingleDenotation.bringForward(Denotations.scala:748)
	dotty.tools.dotc.core.Denotations$SingleDenotation.toNewRun$1(Denotations.scala:805)
	dotty.tools.dotc.core.Denotations$SingleDenotation.current(Denotations.scala:876)
	dotty.tools.dotc.core.Symbols$Symbol.recomputeDenot(Symbols.scala:122)
	dotty.tools.dotc.core.Symbols$Symbol.computeDenot(Symbols.scala:116)
	dotty.tools.dotc.core.Symbols$Symbol.denot(Symbols.scala:109)
	dotty.tools.dotc.core.Symbols$.toDenot(Symbols.scala:531)
	dotty.tools.dotc.typer.Checking.checkLegalImportPath(Checking.scala:1020)
	dotty.tools.dotc.typer.Checking.checkLegalImportPath$(Checking.scala:884)
	dotty.tools.dotc.typer.Typer.checkLegalImportPath(Typer.scala:120)
	dotty.tools.dotc.typer.Typer.typedImport(Typer.scala:2896)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3126)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3197)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3274)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3278)
	dotty.tools.dotc.typer.Typer.traverse$1(Typer.scala:3290)
	dotty.tools.dotc.typer.Typer.typedStats(Typer.scala:3346)
	dotty.tools.dotc.typer.Typer.typedPackageDef(Typer.scala:2923)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3147)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3197)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3274)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3278)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3389)
	dotty.tools.dotc.typer.TyperPhase.typeCheck$$anonfun$1(TyperPhase.scala:47)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	dotty.tools.dotc.core.Phases$Phase.monitor(Phases.scala:477)
	dotty.tools.dotc.typer.TyperPhase.typeCheck(TyperPhase.scala:53)
	dotty.tools.dotc.typer.TyperPhase.$anonfun$4(TyperPhase.scala:99)
	scala.collection.Iterator$$anon$6.hasNext(Iterator.scala:479)
	scala.collection.Iterator$$anon$9.hasNext(Iterator.scala:583)
	scala.collection.immutable.List.prependedAll(List.scala:152)
	scala.collection.immutable.List$.from(List.scala:684)
	scala.collection.immutable.List$.from(List.scala:681)
	scala.collection.IterableOps$WithFilter.map(Iterable.scala:898)
	dotty.tools.dotc.typer.TyperPhase.runOn(TyperPhase.scala:100)
	dotty.tools.dotc.Run.runPhases$1$$anonfun$1(Run.scala:315)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.ArrayOps$.foreach$extension(ArrayOps.scala:1323)
	dotty.tools.dotc.Run.runPhases$1(Run.scala:337)
	dotty.tools.dotc.Run.compileUnits$$anonfun$1(Run.scala:350)
	dotty.tools.dotc.Run.compileUnits$$anonfun$adapted$1(Run.scala:360)
	dotty.tools.dotc.util.Stats$.maybeMonitored(Stats.scala:69)
	dotty.tools.dotc.Run.compileUnits(Run.scala:360)
	dotty.tools.dotc.Run.compileSources(Run.scala:261)
	dotty.tools.dotc.interactive.InteractiveDriver.run(InteractiveDriver.scala:161)
	dotty.tools.pc.MetalsDriver.run(MetalsDriver.scala:47)
	dotty.tools.pc.HoverProvider$.hover(HoverProvider.scala:38)
	dotty.tools.pc.ScalaPresentationCompiler.hover$$anonfun$1(ScalaPresentationCompiler.scala:345)
```
#### Short summary: 

java.lang.AssertionError: assertion failed: denotation object language invalid in run 3. ValidFor: Period(4.1-2)