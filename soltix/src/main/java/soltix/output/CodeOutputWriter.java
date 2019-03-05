/*
 * SOLTIX - Scalable automated framework for testing Solidity compilers.
 *
 * Author: Nils Weller <nweller@uni-bremen.de>
 *
 * Copyright (C) 2018 Secure, Reliable, and Intelligent Systems Lab, ETH Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package soltix.output;

import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.ArrayList;

public class CodeOutputWriter {
    final static Logger logger = Logger.getLogger(CodeOutputWriter.class);

    private PrintWriter printWriter = null;
    private ArrayList<String> memoryList = null;
    private String currentMemoryLine = null;
    private int lineNumber = 1;
    private int linePosition = 0;

    public CodeOutputWriter(PrintWriter printWriter) {
        if (printWriter != null) {
            this.printWriter = printWriter;
        } else {
            memoryList = new ArrayList<String>();
        }
    }

    protected void doPrint(String s, boolean newline) throws Exception {
        if (printWriter != null) {
            printWriter.print(s);
            if (newline) {
                printWriter.println();
            }
        } else {
            if (currentMemoryLine == null) {
                currentMemoryLine = "";
            }
            currentMemoryLine += s; // newline not appended!
        }
    }

    public void print(String s) throws Exception {
        if (s.contains("\n")) {
            throw new Exception("Unexpected newline in CodeOutputWriter.print() - use println instead");
        }
        doPrint(s, false);
        linePosition += s.length();
    }

    public void println(String s) throws Exception {
        doPrint(s, true);
        linePosition = 0;
        if (printWriter == null) {
            memoryList.add(currentMemoryLine);
            currentMemoryLine = null;
        }
        ++lineNumber;
    }

    public void flush() {
        if (printWriter != null) {
            printWriter.flush();
        }
    }

    public int getCurrentLineNumber() {
        return lineNumber;
    }

    public int getCurrentLinePosition() {
        return linePosition;
    }

    public String getMemoryLine(int number) throws Exception {
        --number; // 1-based counting
        if (memoryList == null) {
            throw new Exception("getMemoryLine called on non-memory CodeOutputWriter");
        }
        return memoryList.get(number);
    }

    public void printLineToLogger(int lineNumber, long statementID) throws Exception {
        String line = getMemoryLine(lineNumber);
        logger.info("" + lineNumber + "    [S " + statementID + "]    " + line);
    }

}
