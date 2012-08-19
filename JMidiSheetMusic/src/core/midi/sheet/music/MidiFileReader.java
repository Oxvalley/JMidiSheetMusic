package core.midi.sheet.music;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @class MidiFileReader The MidiFileReader is used to read low-level binary data
 *        from a file. This class can do the following:
 * 
 *        - Peek at the next byte in the file. - Read a byte - Read a 16-bit big
 *        endian short - Read a 32-bit big endian int - Read a fixed length
 *        ascii string (not null terminated) - Read a "variable length" integer.
 *        The format of the variable length int is described at the top of this
 *        file. - Skip ahead a given number of bytes - Return the current
 *        offset.
 */

public class MidiFileReader
{
   private char[] data;
   /** The entire midi file data */
   private int parse_offset;

   /** The current offset while parsing */

   /**
    * Create a new MidiFileReader for the given filename
    * 
    * @throws MidiFileException
    * @throws IOException
    */
   public MidiFileReader(String filename) throws MidiFileException, IOException
   {
      File info = new File(filename);
      if (!info.exists())
      {
         throw new MidiFileException("File " + filename + " does not exist", 0);
      }
      if (info.length() == 0)
      {
         throw new MidiFileException(
               "File " + filename + " is empty (0 bytes)", 0);
      }
      FileReader file = new FileReader(filename);

      /* Read the entire file into memory */
      // TODO LS: What if size is larger than int??
      data = new char[(int) info.length()];
      int offset = 0;
      // int len = (int)info.length();
      while (true)
      {
         if (offset == info.length())
            break;
         int n = file.read(data, offset, (int) (info.length() - offset));
         if (n <= 0)
            break;
         offset += n;
      }
      parse_offset = 0;
      file.close();
   }

   /** Create a new MidiFileReader from the given data */
   public MidiFileReader(char[] bytes)
   {
      data = bytes;
      parse_offset = 0;
   }

   /**
    * Check that the given number of bytes doesn't exceed the file size
    * 
    * @throws MidiFileException
    */
   private void checkRead(int amount) throws MidiFileException
   {
      if (parse_offset + amount > data.length)
      {
         throw new MidiFileException("File is truncated", parse_offset);
      }
   }

   /**
    * Read the next byte in the file, but don't increment the parse offset
    * 
    * @throws MidiFileException
    */
   public char Peek() throws MidiFileException
   {
      checkRead(1);
      return data[parse_offset];
   }

   /**
    * Read a byte from the file
    * 
    * @throws MidiFileException
    */
   public char ReadByte() throws MidiFileException
   {
      checkRead(1);
      char x = data[parse_offset];
      parse_offset++;
      return x;
   }

   /**
    * Read the given number of bytes from the file
    * 
    * @throws MidiFileException
    */
   public char[] ReadBytes(int amount) throws MidiFileException
   {
      checkRead(amount);
      char[] result = new char[amount];
      for (int i = 0; i < amount; i++)
      {
         result[i] = data[i + parse_offset];
      }
      parse_offset += amount;
      return result;
   }

   /**
    * Read a 16-bit short from the file
    * 
    * @throws MidiFileException
    */
   public int ReadShort() throws MidiFileException
   {
      checkRead(2);
      int x = ((data[parse_offset] << 8) | data[parse_offset + 1]);
      parse_offset += 2;
      return x;
   }

   /**
    * Read a 32-bit int from the file
    * 
    * @throws MidiFileException
    */
   public int ReadInt() throws MidiFileException
   {
      checkRead(4);
      int x = (data[parse_offset] << 24) | (data[parse_offset + 1] << 16)
            | (data[parse_offset + 2] << 8) | data[parse_offset + 3];
      parse_offset += 4;
      return x;
   }

   /**
    * Read an ascii string with the given length
    * 
    * @throws MidiFileException
    */
   public String ReadAscii(int len) throws MidiFileException
   {
      checkRead(len);
      String s = new String(data, parse_offset, len);
      parse_offset += len;
      return s;
   }

   /**
    * Read a variable-length integer (1 to 4 bytes). The integer ends when you
    * encounter a byte that doesn't have the 8th bit set (a byte less than
    * 0x80).
    * 
    * @throws MidiFileException
    */
   public int ReadVarlen() throws MidiFileException
   {
      int result = 0;
      char b;

      b = ReadByte();
      result = b & 0x7f;

      for (int i = 0; i < 3; i++)
      {
         if ((b & 0x80) != 0)
         {
            b = ReadByte();
            result = (result << 7) + (b & 0x7f);
         }
         else
         {
            break;
         }
      }
      return result;
   }

   /**
    * Skip over the given number of bytes
    * 
    * @throws MidiFileException
    */
   public void Skip(int amount) throws MidiFileException
   {
      checkRead(amount);
      parse_offset += amount;
   }

   /** Return the current parse offset */
   public int GetOffset()
   {
      return parse_offset;
   }

   /** Return the raw midi file byte data */
   public char[] GetData()
   {
      return data;
   }
}
