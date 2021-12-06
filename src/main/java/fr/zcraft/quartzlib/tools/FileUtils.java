/*
 * Copyright or © or Copr. QuartzLib contributors (2015 - 2020)
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.zcraft.quartzlib.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public final class FileUtils {
    private FileUtils() {
    }

    /**
     * Reads a file, returns its content into a String. Returns an empty string if the file is
     * unreachable.
     *
     * @param file The file to read.
     * @return The file content.
     */
    public static String readFile(File file) {
        BufferedReader reader = null;

        try {
            StringBuilder content = new StringBuilder();
            reader = new BufferedReader(new FileReader(file));

            for (String line; (line = reader.readLine()) != null; ) {
                content.append(line).append('\n');
            }

            return content.toString();
        } catch (IOException e) {
            return "";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Writes the given content into the file, replacing its content.
     *
     * @param file    The file to write into.
     * @param content The content to write into the file.
     */
    public static void writeFile(File file, String content) throws IOException {
        writeFile(file, content, true);
    }

    /**
     * Writes the given content into the file.
     *
     * @param file      The file to write into.
     * @param content   The content to write into the file.
     * @param overwrite {@code true} to overwrite, {@code false} to append.
     * @throws IOException if the file cannot be written to.
     */
    public static void writeFile(File file, String content, boolean overwrite) throws IOException {
        FileWriter writer = null;

        try {
            writer = new FileWriter(file, !overwrite);
            writer.write(content);
            writer.close();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
