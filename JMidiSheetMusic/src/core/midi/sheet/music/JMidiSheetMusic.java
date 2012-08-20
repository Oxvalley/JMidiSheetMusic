package core.midi.sheet.music;

/*
 * C# original Copyright (c) 2007-2012 Madhav Vaidyanathan
 * Java port   Copyright(c) 2012 Lars Svensson
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2.
 *
 *  This program is distributed : the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */

public class JMidiSheetMusic
{

   /* Check if the Timidity MIDI player is installed */
   private static void CheckTimidity() {
//       FileInfo info = new FileInfo("/usr/bin/timidity");
//       if (!info.Exists) {
//           String message = "The Timidity MIDI player is not installed.\n";
//           message += "Therefore, the MIDI audio sound will not work.\n";
//           message += "To install Timidity on Ubuntu Linux, run the command:\n";
//           message += "# sudo apt-get install timidity\n";
//
//           MessageBox.Show(message, "MIDI Audio is disabled",
//                           MessageBoxButtons.OK, MessageBoxIcon.Error);
     }
   
   
   /** The Main function for this application. */
   //[STAThread]
   public static void main(String[] args)
   {       
      // Application.EnableVisualStyles();
       JSheetMusicWindow form = new JSheetMusicWindow();
//       if (Type.GetType("Mono.Runtime") != null) {
//           CheckTimidity();
//       }
       if (args.length == 1) {
           form.OpenMidiFile(args[0]);
       }
       form.setVisible(true);
   }

}
