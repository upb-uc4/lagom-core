package de.upb.cs.uc4.hyperledger

import java.io.File
import java.nio.file.{ Path, Paths }

import com.typesafe.config.Config

trait HyperledgerDefaultParts {

  implicit val config: Config

  protected val walletPath: Path = retrieveFolderPathWithCreation("uc4.hyperledger.walletPath", "/hyperledger_assets/wallet/")
  protected val networkDescriptionPath: Path = retrievePath("uc4.hyperledger.networkConfig", "/hyperledger_assets/connection_profile.yaml")
  protected val tlsCert: Path = retrievePath("uc4.hyperledger.tlsCert", "")

  protected val channel: String = retrieveString("uc4.hyperledger.channel")
  protected val chaincode: String = retrieveString("uc4.hyperledger.chaincode")
  protected val caURL: String = retrieveString("uc4.hyperledger.caURL")

  /** Retrieves the path from the key out of the configuration.
    *
    * @param key in the configuration
    * @param fallback the path as string if the key does not exist
    * @return the retrieved path
    */
  protected def retrievePath(key: String, fallback: String): Path = {
    if (config.hasPath(key)) {
      Paths.get(config.getString(key))
    }
    else {
      Paths.get(getClass.getResource(fallback).toURI)
    }
  }

  /** Retrieves the path from the key out of the configuration and
    * creates the folder if it does not exist.
    *
    * @param key in the configuration
    * @param fallback the path as string if the key does not exist
    * @return the retrieved path
    */
  protected def retrieveFolderPathWithCreation(key: String, fallback: String): Path = {
    if (config.hasPath(key)) {
      val directory = new File(config.getString(key))
      if (!directory.exists()) {
        directory.mkdirs()
      }
      Paths.get(config.getString(key))
    }
    else {
      Paths.get(getClass.getResource(fallback).toURI)
    }
  }

  /** Retrieves a string from the key out of the configuration.
    *
    * @param key in the configuration
    * @param fallback used if the key does not exist
    * @return the retrieved string
    */
  protected def retrieveString(key: String, fallback: String = ""): String = {
    if (config.hasPath(key)) {
      config.getString(key)
    }
    else {
      fallback
    }
  }
}
