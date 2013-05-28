/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2012 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.objectweb.proactive.core.util.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.objectweb.proactive.core.mop.PAObjectOutputStream;
import org.objectweb.proactive.core.mop.SunMarshalOutputStream;
import org.objectweb.proactive.core.runtime.ProActiveRuntimeImpl;
import org.objectweb.proactive.core.util.converter.MakeDeepCopy.ConversionMode;


/**
 * This class acts as a wrapper to enable the use of different serialization code
 * depending on the proactive configuration
 *
 */
public class ObjectToByteConverter {

    static {
        // resolve PROACTIVE-742
        ProActiveRuntimeImpl.getProActiveRuntime();
    }

    public static class MarshallStream {

        /**
         * Convert to an object using a marshall stream;
         * @param byteArray the byte array to covnert
         * @return the unserialized object
         * @throws IOException
         * @throws ClassNotFoundException
         */

        /**
         * Convert an object to a byte array using a marshall stream
         * @param o The object to convert.
         * @return The object converted to a byte array
         * @throws IOException
         */
        public static byte[] convert(Object o) throws IOException {
            return ObjectToByteConverter.convert(o, ConversionMode.MARSHALL);
        }
    }

    public static class ObjectStream {

        /**
         * Convert an object to a byte array using a regular object stream
         * @param o The object to convert.
         * @return The object converted to a byte array
         * @throws IOException
         */
        public static byte[] convert(Object o) throws IOException {
            return ObjectToByteConverter.convert(o, ConversionMode.OBJECT);
        }
    }

    public static class ProActiveObjectStream {

        /**
         * Convert an object to a byte array using a proactive object stream
         * @param o The object to convert.
         * @return The object converted to a byte array
         * @throws IOException
         */
        public static byte[] convert(Object o) throws IOException {
            return ObjectToByteConverter.convert(o, ConversionMode.PAOBJECT);
        }
    }

    private static byte[] convert(Object o, ConversionMode conversionMode) throws IOException {
        return standardConvert(o, conversionMode);
    }

    private static void writeToStream(ObjectOutputStream objectOutputStream, Object o) throws IOException {
        objectOutputStream.writeObject(o);
        objectOutputStream.flush();
    }

    private static byte[] standardConvert(Object o, ConversionMode conversionMode) throws IOException {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
        ObjectOutputStream objectOutputStream = null;

        try {
            // we use enum and static calls to avoid object instanciation
            if (conversionMode == ConversionMode.MARSHALL) {
                objectOutputStream = new SunMarshalOutputStream(byteArrayOutputStream);
            } else if (conversionMode == ConversionMode.OBJECT) {
                objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            } else if (conversionMode == ConversionMode.PAOBJECT) {
                objectOutputStream = new PAObjectOutputStream(byteArrayOutputStream);
            }

            ObjectToByteConverter.writeToStream(objectOutputStream, o);
            return byteArrayOutputStream.toByteArray();
        } finally {
            // close streams
            if (objectOutputStream != null) {
                objectOutputStream.close();
            }
            byteArrayOutputStream.close();
        }
    }
}
