/* ===================== COPYRIGHT NOTICE =====================
 * This file is protected by Copyright. Please refer to the COPYRIGHT file
 * distributed with this source distribution.
 *
 * This file is part of REDHAWK.
 *
 * REDHAWK is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * REDHAWK is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * ============================================================
 */

package nxm.vrt.dat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// NOTE:
//  This file is in the 'nxm.vrt.dat' package temporarially. It will probably
//  move to a "developer tools" area in the very near future. Depending on
//  the final disposition of this class, it may be updated to extend
//  nxm.vrt.lib.Main.

/** <b>Internal Use Only:</b> Fixes the copyright notice at the top of a source code file which
 *  makes it easy to automate most updates (such as changing the year). From the command-line
 *  this takes in a file name pointing to the new copyright notice to use (or a file with the
 *  correct copyright notice already present) and a list of files to apply it to.
 */
public final class FixNotice {
  // The next two lines have a "+" in them to prevent matching when THIS file is updated
  private static final String COPYRIGHT_START = "====================="+" COPYRIGHT NOTICE "+"=====================";
  private static final String COPYRIGHT_END   = "====================="+"=================="+"=====================";

  // Default copyright notices
  private static final String[] DEF_JAVA_COMMENT  = { "/* "+COPYRIGHT_START,
                                                      " * "+COPYRIGHT_END,
                                                      " */",
                                                      "" };
  private static final String[] DEF_MAKE_COMMENT  = { "#",
                                                      "# "+COPYRIGHT_START,
                                                      "# "+COPYRIGHT_END,
                                                      "#" };
  private static final String[] DEF_MIDAS_COMMENT = { "!!! "+COPYRIGHT_START,
                                                      "!!! "+COPYRIGHT_END };
  private static final String[] DEF_TXT_COMMENT   = { COPYRIGHT_START, COPYRIGHT_END };
  private static final String[] DEF_XML_COMMENT   = { "<!-- ", COPYRIGHT_START, COPYRIGHT_END, "-->" };


  private final List<String> copyrightNotice; // The copyright notice to apply

  /** Creates a new instance.
   *  @param fname The name of the file with the new copyright notice in it.
   *  @throws IOException If there is an error reading the copyright notice file.
   */
  FixNotice (File fname) throws IOException {
    BufferedReader in    = new BufferedReader(new FileReader(fname));
    List<String>   lines = new ArrayList<String>(32);
    String         line;

    while ((line = in.readLine()) != null) {
      lines.add(line);
    }
    in.close();

    int start = findExistingNoticeStart(fname, lines);
    if (start < 0) {
      copyrightNotice = lines;
    }
    else {
      int end = findExistingNoticeEnd(fname, lines, start);
      copyrightNotice = lines.subList(start+1, end);
    }
  }

  /** Fixes the copyright notice.
   *  @param args Command-line arguments. The first must be the file that includes the copyright
   *              notice, all others are files to apply the copyright notice to.
   *  @throws IOException If an i/o error occurs.
   */
  public static void main (String... args) throws IOException {
    if (args.length == 0) {
      System.err.println("ERROR: Invalid usage");
      System.err.println("");
      System.err.println("Usage:   java nxm.vrt.dat.FixNotice <IN> [FNAME]*");
      System.err.println("");
      System.err.println("  <IN>    Input file wiich contains the correct copyright notice.");
      System.err.println("  [FNAME] Name of file(s) to apply copyright notice to.");
    }
    else {
      FixNotice fixNotice = new FixNotice(new File(args[0]));
      for (int i = 1; i < args.length; i++) {
        fixNotice.fixCopyrightNotice(new File(args[i]));
      }
    }
  }

  /** Fixes the copyright notice for a file.
   *  @param fname File name (used with error messages).
   *  @param type  File type.
   *  @param lines Lines from file.
   *  @throws IOException If an i/o error occurs.
   */
  private void fixCopyrightNotice (File fname) throws IOException {
    List<String> inLines = new ArrayList<String>(128);
    String       line;

    BufferedReader in = new BufferedReader(new FileReader(fname));
    while ((line = in.readLine()) != null) {
      inLines.add(line);
    }
    in.close();

    List<String> outLines = fixCopyrightNotice(fname, inLines);
    PrintWriter out = new PrintWriter(new FileWriter(fname));
    for (String l : outLines) {
      out.println(l);
    }
    out.close();
  }

  /** Fixes the copyright notice for a file.
   *  @param fname File name (used with error messages).
   *  @param type  File type.
   *  @param lines Lines from file.
   *  @return Lines from file with fixed copyright notice.
   *  @throws IOException
   */
  private List<String> fixCopyrightNotice (File fname, List<String> lines) throws IOException {
    int start = findExistingNoticeStart(fname, lines);
    if (start < 0) {
      insertDefaultComment(fname, lines);
      start = findExistingNoticeStart(fname, lines);
    }
    int    end    = findExistingNoticeEnd(fname, lines, start);
    String indent = getIndent(fname, lines, start);

    ArrayList<String> fixedLines = new ArrayList<String>(lines.size() + copyrightNotice.size());
    fixedLines.addAll(lines.subList(0, start+1));
    for (String line : copyrightNotice) {
      fixedLines.add(indent + line);
    }
    fixedLines.addAll(lines.subList(end, lines.size()));

    return fixedLines;
  }

  /** Finds the existing copyright notice.
   *  @param fname File name (used with error messages).
   *  @param type  File type.
   *  @param lines Lines from file.
   *  @param start Line number where copyright notice starts.
   *  @return Line number where notice begins or -1 if not found.
   *  @throws IOException if copyright notice end found first.
   */
  private String getIndent (File fname, List<String> lines, int start) {
    String line  = lines.get(start);
    int    index = line.indexOf(COPYRIGHT_START);

    if (index < 0) {
      throw new IllegalArgumentException("Illegal use of getIndent(..)");
    }

    String indent = line.substring(0,index);
    return indent.replace("/*", " *");
  }

  /** Insert the default comment into the file, returns the insertion point.
   *  @param fname File name (used with error messages).
   *  @param type  File type (not used).
   *  @param lines Lines from file.
   *  @return Insertion point.
   */
  private int insertDefaultComment (File fname, List<String> lines) throws IOException {
    int      insertAt = 0;
    String[] text;
    String   name = fname.getName();
    int      idx  = name.lastIndexOf('.');
    String   ext  = (idx < 0)? "" : name.substring(idx);

         if (ext.equals(".c"           )) text = DEF_JAVA_COMMENT;
    else if (ext.equals(".cc"          )) text = DEF_JAVA_COMMENT;
    else if (ext.equals(".css"         )) text = DEF_JAVA_COMMENT;
    else if (ext.equals(".h"           )) text = DEF_JAVA_COMMENT;
    else if (ext.equals(".html"        )) text = DEF_XML_COMMENT;
    else if (ext.equals(".java"        )) text = DEF_JAVA_COMMENT;
    else if (ext.equals(".mm"          )) text = DEF_MIDAS_COMMENT;
    else if (ext.equals(".xml"         )) text = DEF_XML_COMMENT;
    else if (name.equals("README"      )) text = DEF_TXT_COMMENT;
    else if (name.equals("commands.cfg")) text = DEF_MIDAS_COMMENT;
    else if (name.equals("commands.cnf")) text = DEF_MIDAS_COMMENT;
    else if (name.equals("doxygen.cfg" )) text = DEF_MAKE_COMMENT;
    else if (name.startsWith("Makefile")) text = DEF_MAKE_COMMENT;
    else if (ext.equals(".txt") &&
             name.contains("/mcr/"     )) text = DEF_MIDAS_COMMENT;
    else if (ext.equals(".txt"         )) text = DEF_TXT_COMMENT;
    else throw new IOException("Could not identify type for "+fname);

    if (text == DEF_XML_COMMENT) {
      // Permit "<!DOCTYPE ...>" and "<?xml ...>" to come first
      for (; insertAt < lines.size(); insertAt++) {
        String line = lines.get(insertAt).trim();
        if (line.startsWith("<!DOCTYPE ") || line.startsWith("<?xml ")) {
          int end = line.indexOf('>');
          if (end == line.length()-1) continue;
        }
        break;
      }
    }
    lines.addAll(insertAt, Arrays.asList(text));
    return insertAt;
  }

  /** Finds the existing copyright notice.
   *  @param fname File name (used with error messages).
   *  @param lines Lines from file.
   *  @return Line number where notice begins or -1 if not found.
   *  @throws IOException if copyright notice end found first.
   */
  private int findExistingNoticeStart (File fname, List<String> lines) throws IOException {
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line.contains(COPYRIGHT_START)) {
        return i;
      }
    }
    return -1;
  }

  /** Finds the existing copyright notice.
   *  @param fname File name (used with error messages).
   *  @param lines Lines from file.
   *  @param start Line number where copyright notice starts.
   *  @return Line number where notice begins or -1 if not found.
   *  @throws IOException if copyright notice end found first.
   */
  private int findExistingNoticeEnd (File fname, List<String> lines, int start)
                                    throws IOException {
    for (int i = start+1; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line.contains(COPYRIGHT_END)) {
        return i;
      }
      if (line.contains(COPYRIGHT_START)) {
        throw new IOException("Malformed copyright notice "+fname+"("+(start+1)+")");
      }
    }
    throw new IOException("Malformed copyright notice "+fname+"("+(start+1)+")");
  }

}
