sonatypeProfileName := "com.github.romix"

import ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,   
  runTest,    
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease, 
  ReleaseStep(action = Command.process("publishSigned", _)),
  setNextVersion, 
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  pushChanges
)
