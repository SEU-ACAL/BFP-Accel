// import Mill dependency
import mill._
import mill.scalalib._
import mill.define.Sources
import mill.modules.Util
import mill.scalalib.TestModule.ScalaTest
// support BSP
import mill.bsp._
//
import os.Path

object main extends ScalaModule with SbtModule { m =>
	override def millSourcePath = os.pwd
	override def scalaVersion = "2.13.8"
	override def scalacOptions = Seq(
		"-language:reflectiveCalls",
		"-deprecation",
		"-feature",
		"-Xcheckinit",
		"-P:chiselplugin:genBundleElements"
	)
	override def ivyDeps = Agg(
		ivy"edu.berkeley.cs::chisel3:3.5.4",
		ivy"com.sifive::chisel-circt:0.6.0",
	)
	override def scalacPluginIvyDeps = Agg(
		ivy"edu.berkeley.cs:::chisel3-plugin:3.5.4",
	)
    object test extends SbtModuleTests with TestModule.ScalaTest {
            override def ivyDeps = m.ivyDeps() ++ Agg(
            ivy"org.scalatest::scalatest::3.2.16"
        )
    }
}
