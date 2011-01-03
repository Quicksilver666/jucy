package uc;

import javax.net.ssl.SSLEngine;

import uc.crypto.HashValue;

public interface ICryptoManager {

	public abstract HashValue getFingerPrint();

	public abstract boolean isTLSInitialized();

	public abstract SSLEngine createSSLEngine();

}