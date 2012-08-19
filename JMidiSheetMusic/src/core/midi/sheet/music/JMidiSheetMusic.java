package core.midi.sheet.music;

public class JMidiSheetMusic
{

   /* Check if the Timidity MIDI player is installed */
   private static void CheckTimidity() {
//       FileInfo info = new FileInfo("/usr/bin/timidity");
//       if (!info.Exists) {
//           string message = "The Timidity MIDI player is not installed.\n";
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
