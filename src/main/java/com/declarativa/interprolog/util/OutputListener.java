/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.util;

import java.nio.charset.Charset;

/** Interface by which an OutputHandler client can receive output to analyse */
public interface OutputListener {
	/** nBytes new output bytes are in buffer to analyse 
	 * @param originStd 
	 * @param charset character set assumed by the logic engine */
	public void analyseBytes(byte[] buffer,int nbytes, String originStd, Charset charset);
	public void streamEnded(String originStd);
}
