/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.ingest.sip;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import edu.unc.lib.dl.ingest.IngestException;

/**
 * @author Gregory Jansen
 * 
 */
public class METSParseException extends IngestException implements ErrorHandler {

	/**
     *
     */
	private static final long serialVersionUID = 1L;
	private List<SAXParseException> warnings = new ArrayList<SAXParseException>();

	public List<SAXParseException> getWarnings() {
		return warnings;
	}

	public List<SAXParseException> getErrors() {
		return errors;
	}

	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder();
		sb.append(msg);
		for(SAXParseException e : this.fatalErrors) {
			sb.append("\n").append(e.getMessage());
		}
		for(SAXParseException e : this.errors) {
			sb.append("\n").append(e.getMessage());
		}
		for(SAXParseException e : this.warnings) {
			sb.append("\n").append(e.getMessage());
		}
		return sb.toString();
	}

	public List<SAXParseException> getFatalErrors() {
		return fatalErrors;
	}

	private List<SAXParseException> errors = new ArrayList<SAXParseException>();
	private List<SAXParseException> fatalErrors = new ArrayList<SAXParseException>();
	private static final String msg = "There was a problem parsing METS XML.";

	/**
	 * @param msg
	 */
	public METSParseException() {
		super("There was a problem parsing METS XML.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
	 */
	@Override
	public void error(SAXParseException exception) throws SAXException {
		this.errors.add(exception);
		throw exception;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
	 */
	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		this.fatalErrors.add(exception);
		throw exception;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
	 */
	@Override
	public void warning(SAXParseException exception) throws SAXException {
		this.warnings.add(exception);
		throw exception;
	}

}
