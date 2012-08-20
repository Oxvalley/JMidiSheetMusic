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

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

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


   /* This file contains the classes for parsing and modifying
    * MIDI music files.
    */

   /* MIDI file format.
    *
    * The Midi File format is described below.  The description uses
    * the following abbreviations.
    *
    * u1     - One char
    * u2     - Two chars (big endian)
    * u4     - Four chars (big endian)
    * varlen - A variable length integer, that can be 1 to 4 chars. The 
    *          integer ends when you encounter a char that doesn't have 
    *          the 8th bit set (a char less than 0x80).
    * len?   - The length of the data depends on some code
    *          
    *
    * The Midi files begins with the main Midi header
    * u4 = The four ascii characters 'MThd'
    * u4 = The length of the MThd header = 6 chars
    * u2 = 0 if the file contains a single track
    *      1 if the file contains one or more simultaneous tracks
    *      2 if the file contains one or more independent tracks
    * u2 = number of tracks
    * u2 = if >  0, the number of pulses per quarter note
    *      if <= 0, then ???
    *
    * Next come the individual Midi tracks.  The total number of Midi
    * tracks was given above, : the MThd header.  Each track starts
    * with a header:
    *
    * u4 = The four ascii characters 'MTrk'
    * u4 = Amount of track data, : chars.
    * 
    * The track data consists of a series of Midi events.  Each Midi event
    * has the following format:
    *
    * varlen  - The time between the previous event and this event, measured
    *           : "pulses".  The number of pulses per quarter note is given
    *           : the MThd header.
    * u1      - The Event code, always betwee 0x80 and 0xFF
    * len?    - The event data.  The length of this data is determined by the
    *           event code.  The first char of the event data is always < 0x80.
    *
    * The event code is optional.  If the event code is missing, then it
    * defaults to the previous event code.  For example:
    *
    *   varlen, eventcode1, eventdata,
    *   varlen, eventcode2, eventdata,
    *   varlen, eventdata,  // eventcode is eventcode2
    *   varlen, eventdata,  // eventcode is eventcode2
    *   varlen, eventcode3, eventdata,
    *   ....
    *
    *   How do you know if the eventcode is there or missing? Well:
    *   - All event codes are between 0x80 and 0xFF
    *   - The first char of eventdata is always less than 0x80.
    *   So, after the varlen delta time, if the next char is between 0x80
    *   and 0xFF, its an event code.  Otherwise, its event data.
    *
    * The Event codes and event data for each event code are shown below.
    *
    * Code:  u1 - 0x80 thru 0x8F - Note Off event.
    *             0x80 is for channel 1, 0x8F is for channel 16.
    * Data:  u1 - The note number, 0-127.  Middle C is 60 (0x3C)
    *        u1 - The note velocity.  This should be 0
    * 
    * Code:  u1 - 0x90 thru 0x9F - Note On event.
    *             0x90 is for channel 1, 0x9F is for channel 16.
    * Data:  u1 - The note number, 0-127.  Middle C is 60 (0x3C)
    *        u1 - The note velocity, from 0 (no sound) to 127 (loud).
    *             A value of 0 is equivalent to a Note Off.
    *
    * Code:  u1 - 0xA0 thru 0xAF - Key Pressure
    * Data:  u1 - The note number, 0-127.
    *        u1 - The pressure.
    *
    * Code:  u1 - 0xB0 thru 0xBF - Control Change
    * Data:  u1 - The controller number
    *        u1 - The value
    *
    * Code:  u1 - 0xC0 thru 0xCF - Program Change
    * Data:  u1 - The program number.
    *
    * Code:  u1 - 0xD0 thru 0xDF - Channel Pressure
    *        u1 - The pressure.
    *
    * Code:  u1 - 0xE0 thru 0xEF - Pitch Bend
    * Data:  u2 - Some data
    *
    * Code:  u1     - 0xFF - Meta Event
    * Data:  u1     - Metacode
    *        varlen - Length of meta event
    *        u1[varlen] - Meta event data.
    *
    *
    * The Meta Event codes are listed below:
    *
    * Metacode: u1         - 0x0  Sequence Number
    *           varlen     - 0 or 2
    *           u1[varlen] - Sequence number
    *
    * Metacode: u1         - 0x1  Text
    *           varlen     - Length of text
    *           u1[varlen] - Text
    *
    * Metacode: u1         - 0x2  Copyright
    *           varlen     - Length of text
    *           u1[varlen] - Text
    *
    * Metacode: u1         - 0x3  Track Name
    *           varlen     - Length of name
    *           u1[varlen] - Track Name
    *
    * Metacode: u1         - 0x58  Time Signature
    *           varlen     - 4 
    *           u1         - numerator
    *           u1         - log2(denominator)
    *           u1         - clocks : metronome click
    *           u1         - 32nd notes : quarter note (usually 8)
    *
    * Metacode: u1         - 0x59  Key Signature
    *           varlen     - 2
    *           u1         - if >= 0, then number of sharps
    *                        if < 0, then number of flats * -1
    *           u1         - 0 if major key
    *                        1 if minor key
    *
    * Metacode: u1         - 0x51  Tempo
    *           varlen     - 3  
    *           u3         - quarter note length : microseconds
    */


   /** @class MidiFile
    *
    * The MidiFile class contains the parsed data from the Midi File.
    * It contains:
    * - All the tracks : the midi file, including all MidiNotes per track.
    * - The time signature (e.g. 4/4, 3/4, 6/8)
    * - The number of pulses per quarter note.
    * - The tempo (number of microseconds per quarter note).
    *
    * The constructor takes a filename as input, and upon returning,
    * contains the parsed data from the midi file.
    *
    * The methods ReadTrack() and ReadMetaEvent() are helper functions called
    * by the constructor during the parsing.
    *
    * After the MidiFile is parsed and created, the user can retrieve the 
    * tracks and notes by using the property Tracks and Tracks.Notes.
    *
    * There are two methods for modifying the midi data based on the menu
    * options selected:
    *
    * - ChangeMidiNotes()
    *   Apply the menu options to the parsed MidiFile.  This uses the helper functions:
    *     SplitTrack()
    *     CombineToTwoTracks()
    *     ShiftTime()
    *     Transpose()
    *     RoundStartTimes()
    *     RoundDurations()
    *
    * - ChangeSound()
    *   Apply the menu options to the MIDI music data, and save the modified midi data 
    *   to a file, for playback. 
    *   
    */

   public class MidiFile {
       private String filename;          /** The Midi file name */
       private List<MidiEvent> events[]; /** The raw MidiEvents, one list per track */
       private List<MidiTrack> tracks ;  /** The tracks of the midifile that have notes */
       private int trackmode;         /** 0 (single track), 1 (simultaneous tracks) 2 (independent tracks) */
       private TimeSignature timesig;    /** The time signature */
       private int quarternote;          /** The number of pulses per quarter note */
       private int totalpulses;          /** The total length of the song, : pulses */
       private boolean trackPerChannel;     /** True if we've split each channel into a track */

       /* The list of Midi Events */
       public static int EventNoteOff         = 0x80;
       public static int EventNoteOn          = 0x90;
       public static int EventKeyPressure     = 0xA0;
       public static int EventControlChange   = 0xB0;
       public static int EventProgramChange   = 0xC0;
       public static int EventChannelPressure = 0xD0;
       public static int EventPitchBend       = 0xE0;
       public static int SysexEvent1          = 0xF0;
       public static int SysexEvent2          = 0xF7;
       public static int MetaEvent            = 0xFF;

       /* The list of Meta Events */
       public static int MetaEventSequence      = 0x0;
       public static int MetaEventText          = 0x1;
       public static int MetaEventCopyright     = 0x2;
       public static int MetaEventSequenceName  = 0x3;
       public static int MetaEventInstrument    = 0x4;
       public static int MetaEventLyric         = 0x5;
       public static int MetaEventMarker        = 0x6;
       public static int MetaEventEndOfTrack    = 0x2F;
       public static int MetaEventTempo         = 0x51;
       public static int MetaEventSMPTEOffset   = 0x54;
       public static int MetaEventTimeSignature = 0x58;
       public static int MetaEventKeySignature  = 0x59;

       /* The Program Change event gives the instrument that should
        * be used for a particular channel.  The following table
        * maps each instrument number (0 thru 128) to an instrument
        * name.
        */
       public static String[] Instruments = {

           "Acoustic Grand Piano",
           "Bright Acoustic Piano",
           "Electric Grand Piano",
           "Honky-tonk Piano",
           "Electric Piano 1",
           "Electric Piano 2",
           "Harpsichord",
           "Clavi",
           "Celesta",
           "Glockenspiel",
           "Music Box",
           "Vibraphone",
           "Marimba",
           "Xylophone",
           "Tubular Bells",
           "Dulcimer",
           "Drawbar Organ",
           "Percussive Organ",
           "Rock Organ",
           "Church Organ",
           "Reed Organ",
           "Accordion",
           "Harmonica",
           "Tango Accordion",
           "Acoustic Guitar (nylon)",
           "Acoustic Guitar (steel)",
           "Electric Guitar (jazz)",
           "Electric Guitar (clean)",
           "Electric Guitar (muted)",
           "Overdriven Guitar",
           "Distortion Guitar",
           "Guitar harmonics",
           "Acoustic Bass",
           "Electric Bass (finger)",
           "Electric Bass (pick)",
           "Fretless Bass",
           "Slap Bass 1",
           "Slap Bass 2",
           "Synth Bass 1",
           "Synth Bass 2",
           "Violin",
           "Viola",
           "Cello",
           "Contrabass",
           "Tremolo Strings",
           "Pizzicato Strings",
           "Orchestral Harp",
           "Timpani",
           "String Ensemble 1",
           "String Ensemble 2",
           "SynthStrings 1",
           "SynthStrings 2",
           "Choir Aahs",
           "Voice Oohs",
           "Synth Voice",
           "Orchestra Hit",
           "Trumpet",
           "Trombone",
           "Tuba",
           "Muted Trumpet",
           "French Horn",
           "Brass Section",
           "SynthBrass 1",
           "SynthBrass 2",
           "Soprano Sax",
           "Alto Sax",
           "Tenor Sax",
           "Baritone Sax",
           "Oboe",
           "English Horn",
           "Bassoon",
           "Clarinet",
           "Piccolo",
           "Flute",
           "Recorder",
           "Pan Flute",
           "Blown Bottle",
           "Shakuhachi",
           "Whistle",
           "Ocarina",
           "Lead 1 (square)",
           "Lead 2 (sawtooth)",
           "Lead 3 (calliope)",
           "Lead 4 (chiff)",
           "Lead 5 (charang)",
           "Lead 6 (voice)",
           "Lead 7 (fifths)",
           "Lead 8 (bass + lead)",
           "Pad 1 (new age)",
           "Pad 2 (warm)",
           "Pad 3 (polysynth)",
           "Pad 4 (choir)",
           "Pad 5 (bowed)",
           "Pad 6 (metallic)",
           "Pad 7 (halo)",
           "Pad 8 (sweep)",
           "FX 1 (rain)",
           "FX 2 (soundtrack)",
           "FX 3 (crystal)",
           "FX 4 (atmosphere)",
           "FX 5 (brightness)",
           "FX 6 (goblins)",
           "FX 7 (echoes)",
           "FX 8 (sci-fi)",
           "Sitar",
           "Banjo",
           "Shamisen",
           "Koto",
           "Kalimba",
           "Bag pipe",
           "Fiddle",
           "Shanai",
           "Tinkle Bell",
           "Agogo",
           "Steel Drums",
           "Woodblock",
           "Taiko Drum",
           "Melodic Tom",
           "Synth Drum",
           "Reverse Cymbal",
           "Guitar Fret Noise",
           "Breath Noise",
           "Seashore",
           "Bird Tweet",
           "Telephone Ring",
           "Helicopter",
           "Applause",
           "Gunshot",
           "Percussion"
       };
       /* End Instruments */

       /** Return a String representation of a Midi event */
       private String EventName(int ev) {
           if (ev >= EventNoteOff && ev < EventNoteOff + 16)
               return "NoteOff";
           else if (ev >= EventNoteOn && ev < EventNoteOn + 16) 
               return "NoteOn";
           else if (ev >= EventKeyPressure && ev < EventKeyPressure + 16) 
               return "KeyPressure";
           else if (ev >= EventControlChange && ev < EventControlChange + 16) 
               return "ControlChange";
           else if (ev >= EventProgramChange && ev < EventProgramChange + 16) 
               return "ProgramChange";
           else if (ev >= EventChannelPressure && ev < EventChannelPressure + 16)
               return "ChannelPressure";
           else if (ev >= EventPitchBend && ev < EventPitchBend + 16)
               return "PitchBend";
           else if (ev == MetaEvent)
               return "MetaEvent";
           else if (ev == SysexEvent1 || ev == SysexEvent2)
               return "SysexEvent";
           else
               return "Unknown";
       }

       /** Return a String representation of a meta-event */
       private String MetaName(int ev) {
           if (ev == MetaEventSequence)
               return "MetaEventSequence";
           else if (ev == MetaEventText)
               return "MetaEventText";
           else if (ev == MetaEventCopyright)
               return "MetaEventCopyright";
           else if (ev == MetaEventSequenceName)
               return "MetaEventSequenceName";
           else if (ev == MetaEventInstrument)
               return "MetaEventInstrument";
           else if (ev == MetaEventLyric)
               return "MetaEventLyric";
           else if (ev == MetaEventMarker)
               return "MetaEventMarker";
           else if (ev == MetaEventEndOfTrack)
               return "MetaEventEndOfTrack";
           else if (ev == MetaEventTempo)
               return "MetaEventTempo";
           else if (ev == MetaEventSMPTEOffset)
               return "MetaEventSMPTEOffset";
           else if (ev == MetaEventTimeSignature)
               return "MetaEventTimeSignature";
           else if (ev == MetaEventKeySignature)
               return "MetaEventKeySignature";
           else
               return "Unknown";
       }


       /** Get the list of tracks */
       public List<MidiTrack> getTracks() {
            return tracks; 
         }
       

       /** Get the time signature */
       public TimeSignature getTime() {
           return timesig; 
       }

       /** Get the file name */
       public String getFileName() {
           return filename; 
       }

       /** Get the total length (in pulses) of the song */
       public int getTotalPulses() {
           return totalpulses; 
       }


       /** Create a new MidiFile from the file. */
       public MidiFile(String filename) {
           MidiFileReader file = new MidiFileReader(filename);
           parse(file, filename);
       }

       /** Create a new MidiFile from the char[]. */
       public MidiFile(char[] data, String title) {
           MidiFileReader file = new MidiFileReader(data);
           if (title == null)
               title = "";
           parse(file, title);
       }

       /** Parse the given Midi file, and return an instance of this MidiFile
        * class.  After reading the midi file, this object will contain:
        * - The raw list of midi events
        * - The Time Signature of the song
        * - All the tracks : the song which contain notes. 
        * - The number, starttime, and duration of each note.
        */
       public void parse(MidiFileReader file, String filename) {
           String id;
           int len;

           this.filename = filename;
           tracks = new ArrayList<MidiTrack>();
           trackPerChannel = false;

           id = file.ReadAscii(4);
           if (id != "MThd") {
               throw new MidiFileException("Doesn't start with MThd", 0);
           }
           len = file.ReadInt(); 
           if (len !=  6) {
               throw new MidiFileException("Bad MThd header", 4);
           }
           trackmode = file.ReadShort();
           int num_tracks = file.ReadShort();
           quarternote = file.ReadShort(); 

           events = (ArrayList<MidiEvent>[]) new ArrayList[num_tracks];
           for (int tracknum = 0; tracknum < num_tracks; tracknum++) {
               events[tracknum] = ReadTrack(file);
               MidiTrack track = new MidiTrack(events[tracknum], tracknum);
               if (track.getNotes().size() > 0) {
                   tracks.add(track);
               }
           }

           /* Get the length of the song : pulses */
           for(MidiTrack track : tracks) {
               MidiNote last = track.getNotes().get(track.getNotes().size()-1);
               if (this.totalpulses < last.getStartTime() + last.getDuration()) {
                   this.totalpulses = last.getStartTime() + last.getDuration();
               }
           }

           /* If we only have one track with multiple channels, then treat
            * each channel as a separate track.
            */
           if (tracks.size() == 1 && HasMultipleChannels(tracks.get(0))) {
               tracks = SplitChannels(tracks.get(0), events[tracks.get(0).getNumber()]);
               trackPerChannel = true;
           }

           CheckStartTimes(tracks);

           /* Determine the time signature */
           int tempo = 0;
           int numer = 0;
           int denom = 0;
           for (List<MidiEvent> list : events) {
               for (MidiEvent mevent : list) {
                   if (mevent.Metaevent == MetaEventTempo && tempo == 0) {
                       tempo = mevent.Tempo;
                   }
                   if (mevent.Metaevent == MetaEventTimeSignature && numer == 0) {
                       numer = mevent.Numerator;
                       denom = mevent.Denominator;
                   }
               }
           }
           if (tempo == 0) {
               tempo = 500000; /* 500,000 microseconds = 0.05 sec */
           }
           if (numer == 0) {
               numer = 4; denom = 4;
           }
           timesig = new TimeSignature(numer, denom, quarternote, tempo);
       }

       /** Parse a single Midi track into a list of MidiEvents.
        * Entering this function, the file offset should be at the start of
        * the MTrk header.  Upon exiting, the file offset should be at the
        * start of the next MTrk header.
        */
       private List<MidiEvent> ReadTrack(MidiFileReader file) {
           List<MidiEvent> result = new ArrayList<MidiEvent>();
           int starttime = 0;
           String id = file.ReadAscii(4);

           if (id != "MTrk") {
               throw new MidiFileException("Bad MTrk header", file.GetOffset() - 4);
           }
           int tracklen = file.ReadInt();
           int trackend = tracklen + file.GetOffset();

           int EventFlag = 0;

           while (file.GetOffset() < trackend) {

               // If the midi file is truncated here, we can still recover.
               // Just return what we've parsed so far.

               int startoffset, deltatime;
               char peekevent;
               try {
                   startoffset = file.GetOffset();
                   deltatime = file.ReadVarlen();
                   starttime += deltatime;
                   peekevent = file.Peek();
               }
               catch (MidiFileException e) {
                   return result;
               }

               MidiEvent mevent = new MidiEvent();
               result.add(mevent);
               mevent.DeltaTime = deltatime;
               mevent.StartTime = starttime;

               if (peekevent >= EventNoteOff) { 
                   mevent.HasEventflag = true; 
                   mevent.EventFlag = file.ReadByte();
               }

               // Console.WriteLine("offset {0}: event {1} {2} start {3} delta {4}", 
               //                   startoffset, mevent.EventFlag, EventName(mevent.EventFlag), 
               //                   starttime, DeltaTime);

               if (mevent.EventFlag >= EventNoteOn && mevent.EventFlag < EventNoteOn + 16) {
            	   mevent.EventFlag = EventNoteOn;
            	   mevent.Channel = (mevent.EventFlag - EventNoteOn);
            	   mevent.Notenumber = file.ReadByte();
            	   mevent.Velocity = file.ReadByte();
               }
               else if (mevent.EventFlag >= EventNoteOff && mevent.EventFlag < EventNoteOff + 16) {
            	   mevent.EventFlag = EventNoteOff;
            	   mevent.Channel = (char)(mevent.EventFlag - EventNoteOff);
            	   mevent.Notenumber = file.ReadByte();
            	   mevent.Velocity = file.ReadByte();
               }
               else if (mevent.EventFlag >= EventKeyPressure && 
                        mevent.EventFlag < EventKeyPressure + 16) {
            	   mevent.EventFlag = EventKeyPressure;
            	   mevent.Channel = (char)(mevent.EventFlag - EventKeyPressure);
            	   mevent.Notenumber = file.ReadByte();
            	   mevent.KeyPressure = file.ReadByte();
               }
               else if (mevent.EventFlag >= EventControlChange && 
                        mevent.EventFlag < EventControlChange + 16) {
            	   mevent.EventFlag = EventControlChange;
            	   mevent.Channel = (char)(mevent.EventFlag - EventControlChange);
            	   mevent.ControlNum = file.ReadByte();
            	   mevent.ControlValue = file.ReadByte();
               }
               else if (mevent.EventFlag >= EventProgramChange && 
                        mevent.EventFlag < EventProgramChange + 16) {
            	   mevent.EventFlag = EventProgramChange;
            	   mevent.Channel = (char)(mevent.EventFlag - EventProgramChange);
            	   mevent.Instrument = file.ReadByte();
               }
               else if (mevent.EventFlag >= EventChannelPressure && 
                        mevent.EventFlag < EventChannelPressure + 16) {
            	   mevent.EventFlag = EventChannelPressure;
            	   mevent.Channel = (char)(mevent.EventFlag - EventChannelPressure);
            	   mevent.ChanPressure = file.ReadByte();
               }
               else if (mevent.EventFlag >= EventPitchBend && 
                        mevent.EventFlag < EventPitchBend + 16) {
            	   mevent.EventFlag = EventPitchBend;
            	   mevent.Channel = (char)(mevent.EventFlag - EventPitchBend);
            	   mevent.PitchBend = file.ReadShort();
               }
               else if (mevent.EventFlag == SysexEvent1) {
            	   mevent.EventFlag = SysexEvent1;
            	   mevent.Metalength = file.ReadVarlen();
            	   mevent.Value = file.ReadBytes(mevent.Metalength);
               }
               else if (mevent.EventFlag == SysexEvent2) {
            	   mevent.EventFlag = SysexEvent2;
            	   mevent.Metalength = file.ReadVarlen();
            	   mevent.Value = file.ReadBytes(mevent.Metalength);
               }
               else if (mevent.EventFlag == MetaEvent) {
            	   mevent.EventFlag = MetaEvent;
            	   mevent.Metaevent = file.ReadByte();
            	   mevent.Metalength = file.ReadVarlen();
            	   mevent.Value = file.ReadBytes(mevent.Metalength);
                   if (mevent.Metaevent == MetaEventTimeSignature) {
                       if (mevent.Metalength < 2) {
                           // throw new MidiFileException(
                           //  "Meta Event Time Signature len == " + Metalength  + 
                           //  " != 4", file.GetOffset());
                    	   mevent.Numerator = 0;
                    	   mevent.Denominator = 4;
                       }
                       else if (mevent.Metalength >= 2 && mevent.Metalength < 4) {
                    	   mevent.Numerator = mevent.Value[0];
                    	   mevent.Denominator = (int)Math.pow(2,mevent.Value[1]);
                       }
                       else {
                    	   mevent.Numerator = mevent.Value[0];
                    	   mevent.Denominator = (int) Math.pow(2, mevent.Value[1]);
                       }
                   }
                   else if (mevent.Metaevent == MetaEventTempo) {
                       if (mevent.Metalength != 3) {
                           throw new MidiFileException(
                             "Meta Event Tempo len == " + mevent.Metalength +
                             " != 3", file.GetOffset());
                       }
                       mevent.Tempo = ( (mevent.Value[0] << 16) | (mevent.Value[1] << 8) | mevent.Value[2]);
                   }
                   else if (mevent.Metaevent == MetaEventEndOfTrack) {
                       /* break;  */
                   }
               }
               else {
                   throw new MidiFileException("Unknown event " + mevent.EventFlag,
                                                file.GetOffset()-1); 
               }
           }

           return result;
       }

       /** Return true if this track contains multiple channels.
        * If a MidiFile contains only one track, and it has multiple channels,
        * then we treat each channel as a separate track.
        */
       static boolean HasMultipleChannels(MidiTrack track) {
           int channel = track.getNotes().get(0).getChannel();
           for (MidiNote note : track.getNotes()) {
               if (note.getChannel() != channel) {
                   return true;
               }
           }
           return false;
       }

       /** Write a variable length number to the buffer at the given offset.
        * Return the number of chars written.
        */
       static int VarlenTochars(int num, char[] buf, int offset) {
           char b1 = (char) ((num >> 21) & 0x7F);
           char b2 = (char) ((num >> 14) & 0x7F);
           char b3 = (char) ((num >>  7) & 0x7F);
           char b4 = (char) (num & 0x7F);

           if (b1 > 0) {
               buf[offset]   = (char)(b1 | 0x80);
               buf[offset+1] = (char)(b2 | 0x80);
               buf[offset+2] = (char)(b3 | 0x80);
               buf[offset+3] = b4;
               return 4;
           }
           else if (b2 > 0) {
               buf[offset]   = (char)(b2 | 0x80);
               buf[offset+1] = (char)(b3 | 0x80);
               buf[offset+2] = b4;
               return 3;
           }
           else if (b3 > 0) {
               buf[offset]   = (char)(b3 | 0x80);
               buf[offset+1] = b4;
               return 2;
           }
           else {
               buf[offset] = b4;
               return 1;
           }
       }

       /** Write a 4-char integer to data[offset : offset+4] */
       private static void IntTochars(int value, char[] data, int offset) {
           data[offset] = (char)( (value >> 24) & 0xFF );
           data[offset+1] = (char)( (value >> 16) & 0xFF );
           data[offset+2] = (char)( (value >> 8) & 0xFF );
           data[offset+3] = (char)( value & 0xFF );
       }

       /** Calculate the track length (in chars) given a list of Midi events */
       private static int GetTrackLength(List<MidiEvent> events) {
           int len = 0;
           char[] buf = new char[1024];
           for (MidiEvent mevent : events) {
               len += VarlenTochars(mevent.DeltaTime, buf, 0);
               len += 1;  /* for mevent.EventFlag */
               switch (mevent.EventFlag) {
                   case EventNoteOn: len += 2; break;
                   case EventNoteOff: len += 2; break;
                   case EventKeyPressure: len += 2; break;
                   case EventControlChange: len += 2; break;
                   case EventProgramChange: len += 1; break;
                   case EventChannelPressure: len += 1; break;
                   case EventPitchBend: len += 2; break;

                   case SysexEvent1: 
                   case SysexEvent2:
                       len += VarlenTochars(mevent.Metalength, buf, 0); 
                       len += mevent.Metalength;
                       break;
                   case MetaEvent: 
                       len += 1; 
                       len += VarlenTochars(mevent.Metalength, buf, 0); 
                       len += mevent.Metalength;
                       break;
                   default: break;
               }
           }
           return len;
       }
               

       /** Write the given list of Midi events to a stream/file.
        *  This method is used for sound playback, for creating new Midi files
        *  with the tempo, transpose, etc changed.
        *
        *  Return true on success, and false on error.
        */
       private static boolean 
       WriteEvents(FileWriter file, List<MidiEvent>[] events, int trackmode, int quarter) {
           try {
               char[] buf = new char[4096];

               /* Write the MThd, len = 6, track mode, number tracks, quarter note */
               file.write("MThd", 0, 4);
               IntTochars(6, buf, 0);
               file.write(buf, 0, 4);
               buf[0] = (char)(trackmode >> 8); 
               buf[1] = (char)(trackmode & 0xFF);
               file.write(buf, 0, 2);
               buf[0] = 0; 
               buf[1] = (char)events.Length;
               file.write(buf, 0, 2);
               buf[0] = (char)(quarter >> 8); 
               buf[1] = (char)(quarter & 0xFF);
               file.write(buf, 0, 2);

               for (List<MidiEvent> list : events) {
                   /* Write the MTrk header and track length */
                   file.write(ASCIIEncoding.ASCII.Getchars("MTrk"), 0, 4);
                   int len = GetTrackLength(list);
                   IntTochars(len, buf, 0);
                   file.write(buf, 0, 4);

                   for (MidiEvent mevent : list) {
                       int varlen = VarlenTochars(DeltaTime, buf, 0);
                       file.write(buf, 0, varlen);

                       if (mevent.EventFlag == SysexEvent1 ||
                           mevent.EventFlag == SysexEvent2 ||
                           mevent.EventFlag == MetaEvent) {
                           buf[0] = (char) mevent.EventFlag;
                       }
                       else {
                           buf[0] = (char)(mevent.EventFlag + Channel);
                       }
                       file.write(buf, 0, 1);

                       if (mevent.EventFlag == EventNoteOn) {
                           buf[0] = Notenumber;
                           buf[1] = Velocity;
                           file.write(buf, 0, 2);
                       }
                       else if (mevent.EventFlag == EventNoteOff) {
                           buf[0] = Notenumber;
                           buf[1] = Velocity;
                           file.write(buf, 0, 2);
                       }
                       else if (mevent.EventFlag == EventKeyPressure) {
                           buf[0] = Notenumber;
                           buf[1] = KeyPressure;
                           file.write(buf, 0, 2);
                       }
                       else if (mevent.EventFlag == EventControlChange) {
                           buf[0] = ControlNum;
                           buf[1] = ControlValue;
                           file.write(buf, 0, 2);
                       }
                       else if (mevent.EventFlag == EventProgramChange) {
                           buf[0] = Instrument;
                           file.write(buf, 0, 1);
                       }
                       else if (mevent.EventFlag == EventChannelPressure) {
                           buf[0] = ChanPressure;
                           file.write(buf, 0, 1);
                       }
                       else if (mevent.EventFlag == EventPitchBend) {
                           buf[0] = (char)(PitchBend >> 8);
                           buf[1] = (char)(PitchBend & 0xFF);
                           file.write(buf, 0, 2);
                       }
                       else if (mevent.EventFlag == SysexEvent1) {
                           int offset = VarlenTochars(Metalength, buf, 0);
                           Array.Copy(Value, 0, buf, offset, Value.Length);
                           file.write(buf, 0, offset + Value.Length);
                       }
                       else if (mevent.EventFlag == SysexEvent2) {
                           int offset = VarlenTochars(Metalength, buf, 0);
                           Array.Copy(Value, 0, buf, offset, Value.Length);
                           file.write(buf, 0, offset + Value.Length);
                       }
                       else if (mevent.EventFlag == MetaEvent && Metaevent == MetaEventTempo) {
                           buf[0] = Metaevent;
                           buf[1] = 3;
                           buf[2] = (char)((Tempo >> 16) & 0xFF);
                           buf[3] = (char)((Tempo >> 8) & 0xFF);
                           buf[4] = (char)(Tempo & 0xFF);
                           file.write(buf, 0, 5);
                       }
                       else if (mevent.EventFlag == MetaEvent) {
                           buf[0] = Metaevent;
                           int offset = VarlenTochars(Metalength, buf, 1) + 1;
                           Array.Copy(Value, 0, buf, offset, Value.Length);
                           file.write(buf, 0, offset + Value.Length);
                       }
                   }
               }
               file.Close();
               return true;
           }
           catch (IOException e) {
               return false;
           }
       }


       /** Clone the list of MidiEvents */
       private static List<MidiEvent>[] CloneMidiEvents(List<MidiEvent>[] origlist) {
           List<MidiEvent>[] newlist = new List<MidiEvent>[ origlist.Length];
           for (int tracknum = 0; tracknum < origlist.Length; tracknum++) {
               List<MidiEvent> origevents = origlist[tracknum];
               List<MidiEvent> newevents = new List<MidiEvent>(origevents.Count);
               newlist[tracknum] = newevents;
               for (MidiEvent mevent : origevents) {
                   newevents.Add( Clone() );
               }
           }
           return newlist;
       }

       /** Create a new Midi tempo event, with the given tempo  */
       private static MidiEvent CreateTempoEvent(int tempo) {
           MidiEvent mevent = new MidiEvent();
           DeltaTime = 0;
           StartTime = 0;
           Hasmevent.EventFlag = true;
           mevent.EventFlag = MetaEvent;
           Metaevent = MetaEventTempo;
           Metalength = 3;
           Tempo = tempo;
           return mevent;
       }


       /** Search the events for a ControlChange event with the same
        *  channel and control number.  If a matching event is found,
        *   update the control value.  Else, add a new ControlChange event.
        */ 
       private static void 
       UpdateControlChange(List<MidiEvent> newevents, MidiEvent changeEvent) {
    	   for (MidiEvent mevent : newevents) {
               if ((mevent.EventFlag == changeEvent.mevent.EventFlag) &&
                   (Channel == changeEvent.Channel) &&
                   (ControlNum == changeEvent.ControlNum)) {

                   ControlValue = changeEvent.ControlValue;
                   return;
               }
           }
           newevents.Add(changeEvent);
       }

       /** Start the Midi music at the given pause time (in pulses).
        *  Remove any NoteOn/NoteOff events that occur before the pause time.
        *  For other events, change the delta-time to 0 if they occur
        *  before the pause time.  Return the modified Midi Events.
        */
       private static List<MidiEvent>[] 
       StartAtPauseTime(List<MidiEvent>[] list, int pauseTime) {
           List<MidiEvent>[] newlist = new List<MidiEvent>[ list.Length];
           for (int tracknum = 0; tracknum < list.Length; tracknum++) {
               List<MidiEvent> events = list[tracknum];
               List<MidiEvent> newevents = new List<MidiEvent>(events.Count);
               newlist[tracknum] = newevents;

               boolean foundEventAfterPause = false;
               for (MidiEvent mevent : events) {

                   if (StartTime < pauseTime) {
                       if (mevent.EventFlag == EventNoteOn ||
                           mevent.EventFlag == EventNoteOff) {

                           /* Skip NoteOn/NoteOff event */
                       }
                       else if (mevent.EventFlag == EventControlChange) {
                           DeltaTime = 0;
                           UpdateControlChange(newevents, mevent);
                       }
                       else {
                           DeltaTime = 0;
                           newevents.Add(mevent);
                       }
                   }
                   else if (!foundEventAfterPause) {
                       DeltaTime = (StartTime - pauseTime);
                       newevents.Add(mevent);
                       foundEventAfterPause = true;
                   }
                   else {
                       newevents.Add(mevent);
                   }
               }
           }
           return newlist;
       }


       /** Write this Midi file to the given filename.
        * If options is not null, apply those options to the midi events
        * before performing the write.
        * Return true if the file was saved successfully, else false.
        */
       public boolean ChangeSound(String destfile, MidiOptions options) {
           return Write(destfile, options);
       }

       public boolean Write(String destfile, MidiOptions options) {
           try {
               FileStream stream;
               stream = new FileStream(destfile, FileMode.Create);
               boolean result = Write(stream, options);
               stream.Close();
               return result;
           }
           catch (IOException e) {
               return false;
           }
       }

       /** Write this Midi file to the given stream.
        * If options is not null, apply those options to the midi events
        * before performing the write.
        * Return true if the file was saved successfully, else false.
        */
       public boolean Write(Stream stream, MidiOptions options) {
           List<MidiEvent>[] newevents = events;
           if (options != null) {
               newevents = ApplyOptionsToEvents(options);
           }
           return WriteEvents(stream, newevents, trackmode, quarternote);
       }


       /* Apply the following sound options to the midi events:
        * - The tempo (the microseconds per pulse)
        * - The instruments per track
        * - The note number (transpose value)
        * - The tracks to include
        * Return the modified list of midi events.
        */
       private List<MidiEvent>[]
       ApplyOptionsToEvents(MidiOptions options) {
           int i;
           if (trackPerChannel) {
               return ApplyOptionsPerChannel(options);
           }

           /* A midifile can contain tracks with notes and tracks without notes.
            * The options.tracks and options.instruments are for tracks with notes.
            * So the track numbers : 'options' may not match correctly if the
            * midi file has tracks without notes. Re-compute the instruments, and 
            * tracks to keep.
            */
           int num_tracks = events.Length;
           int[] instruments = new int[num_tracks];
           bool[] keeptracks = new bool[num_tracks];
           for (i = 0; i < num_tracks; i++) {
               instruments[i] = 0;
               keeptracks[i] = true;
           }
           for (int tracknum = 0; tracknum < tracks.Count; tracknum++) {
               MidiTrack track = tracks[tracknum];
               int realtrack = track.Number;
               instruments[realtrack] = options.instruments[tracknum];
               if (options.tracks[tracknum] == false || 
                   options.mute[tracknum] == true) {
                   keeptracks[realtrack] = false;
               }
           }

           List<MidiEvent>[] newevents = CloneMidiEvents(events);

           /* Set the tempo at the beginning of each track */
           for (int tracknum = 0; tracknum < newevents.Length; tracknum++) {
               MidiEvent mevent = CreateTempoEvent(options.tempo);
               newevents[tracknum].Insert(0, mevent);
           }

           /* Change the note number (transpose), instrument, and tempo */
           for (int tracknum = 0; tracknum < newevents.Length; tracknum++) {
        	   for (MidiEvent mevent : newevents[tracknum]) {
                   int num = Notenumber + options.transpose;
                   if (num < 0)
                       num = 0;
                   if (num > 127)
                       num = 127;
                   Notenumber = (char)num;
                   if (!options.useDefaultInstruments) {
                       Instrument = (char)instruments[tracknum];
                   }
                   Tempo = options.tempo;
               }
           }

           if (options.pauseTime != 0) {
               newevents = StartAtPauseTime(newevents, options.pauseTime);
           }

           /* Change the tracks to include */
           int count = 0;
           for (int tracknum = 0; tracknum < keeptracks.Length; tracknum++) {
               if (keeptracks[tracknum]) {
                   count++;
               }
           }
           List<MidiEvent>[] result = new List<MidiEvent>[count];
           i = 0;
           for (int tracknum = 0; tracknum < keeptracks.Length; tracknum++) {
               if (keeptracks[tracknum]) {
                   result[i] = newevents[tracknum];
                   i++;
               }
           }
           return result;
       }

       /* Apply the following sound options to the midi events:
        * - The tempo (the microseconds per pulse)
        * - The instruments per track
        * - The note number (transpose value)
        * - The tracks to include
        * Return the modified list of midi events.
        *
        * This Midi file only has one actual track, but we've split that
        * into multiple fake tracks, one per channel, and displayed that
        * to the end-user.  So changing the instrument, and tracks to
        * include, is implemented differently than ApplyOptionsToEvents().
        *
        * - We change the instrument based on the channel, not the track.
        * - We include/exclude channels, not tracks.
        * - We exclude a channel by setting the note volume/velocity to 0.
        */
       private List<MidiEvent>[]
       ApplyOptionsPerChannel(MidiOptions options) {
           /* Determine which channels to include/exclude.
            * Also, determine the instruments for each channel.
            */
           int[] instruments = new int[16];
           bool[] keepchannel = new bool[16];
           for (int i = 0; i < 16; i++) {
               instruments[i] = 0;
               keepchannel[i] = true;
           }
           for (int tracknum = 0; tracknum < tracks.Count; tracknum++) {
               MidiTrack track = tracks[tracknum];
               int channel = track.Notes[0].Channel;
               instruments[channel] = options.instruments[tracknum];
               if (options.tracks[tracknum] == false ||
                   options.mute[tracknum] == true) {

                   keepchannel[channel] = false;
               }
           }
           
           List<MidiEvent>[] newevents = CloneMidiEvents(events);

           /* Set the tempo at the beginning of each track */
           for (int tracknum = 0; tracknum < newevents.Length; tracknum++) {
               MidiEvent mevent = CreateTempoEvent(options.tempo);
               newevents[tracknum].Insert(0, mevent);
           }

           /* Change the note number (transpose), instrument, and tempo */
           for (int tracknum = 0; tracknum < newevents.Length; tracknum++) {
        	   for (MidiEvent mevent : newevents[tracknum]) {
                   int num = Notenumber + options.transpose;
                   if (num < 0)
                       num = 0;
                   if (num > 127)
                       num = 127;
                   Notenumber = (char)num;
                   if (!keepchannel[Channel]) {
                       Velocity = 0;
                   }
                   if (!options.useDefaultInstruments) {
                       Instrument = (char)instruments[Channel];
                   }
                   Tempo = options.tempo;
               }
           }
           if (options.pauseTime != 0) {
               newevents = StartAtPauseTime(newevents, options.pauseTime);
           }
           return newevents;
       }


       /** Apply the given sheet music options to the MidiNotes.
        *  Return the midi tracks with the changes applied.
        */
       public List<MidiTrack> ChangeMidiNotes(MidiOptions options) {
           List<MidiTrack> newtracks = new List<MidiTrack>();

           for (int track = 0; track < tracks.Count; track++) {
               if (options.tracks[track]) {
                   newtracks.Add(tracks[track].Clone() );
               }
           }

           /* To make the sheet music look nicer, we round the start times
            * so that notes close together appear as a single chord.  We
            * also extend the note durations, so that we have longer notes
            * and fewer rest symbols.
            */
           TimeSignature time = timesig;
           if (options.time != null) {
               time = options.time;
           }
           MidiFile.RoundStartTimes(newtracks, options.combineInterval, timesig);
           MidiFile.RoundDurations(newtracks, time.Quarter);

           if (options.twoStaffs) {
               newtracks = MidiFile.CombineToTwoTracks(newtracks, timesig.Measure);
           }
           if (options.shifttime != 0) {
               MidiFile.ShiftTime(newtracks, options.shifttime);
           }
           if (options.transpose != 0) {
               MidiFile.Transpose(newtracks, options.transpose);
           }

           return newtracks;
       }


       /** Shift the starttime of the notes by the given amount.
        * This is used by the Shift Notes menu to shift notes left/right.
        */
       public static void
       ShiftTime(List<MidiTrack> tracks, int amount)
       {
    	   for (MidiTrack track : tracks) {
    		   for (MidiNote note : track.Notes) {
                   note.StartTime += amount;
               }
           }
       }

       /** Shift the note keys up/down by the given amount */
       public static void
       Transpose(List<MidiTrack> tracks, int amount)
       {
    	   for (MidiTrack track : tracks) {
    		   for (MidiNote note : track.Notes) {
                   note.Number += amount;
                   if (note.Number < 0) {
                       note.Number = 0;
                   }
               }
           }
       }

      
       /* Find the highest and lowest notes that overlap this interval (starttime to endtime).
        * This method is used by SplitTrack to determine which staff (top or bottom) a note
        * should go to.
        *
        * For more accurate SplitTrack() results, we limit the interval/duration of this note 
        * (and other notes) to one measure. We care only about high/low notes that are
        * reasonably close to this note.
        */
       private static void
       FindHighLowNotes(List<MidiNote> notes, int measurelen, int startindex, 
                        int starttime, int endtime, ref int high, ref int low) {

           int i = startindex;
           if (starttime + measurelen < endtime) {
               endtime = starttime + measurelen;
           }

           while (i < notes.Count && notes[i].StartTime < endtime) {
               if (notes[i].EndTime < starttime) {
                   i++;
                   continue;
               }
               if (notes[i].StartTime + measurelen < starttime) {
                   i++;
                   continue;
               }
               if (high < notes[i].Number) {
                   high = notes[i].Number;
               }
               if (low > notes[i].Number) {
                   low = notes[i].Number;
               }
               i++;
           }
       }

       /* Find the highest and lowest notes that start at this exact start time */
       private static void
       FindExactHighLowNotes(List<MidiNote> notes, int startindex, int starttime,
                             ref int high, ref int low) {

           int i = startindex;

           while (notes[i].StartTime < starttime) {
               i++;
           }

           while (i < notes.Count && notes[i].StartTime == starttime) {
               if (high < notes[i].Number) {
                   high = notes[i].Number;
               }
               if (low > notes[i].Number) {
                   low = notes[i].Number;
               }
               i++;
           }
       }


    
       /* Split the given MidiTrack into two tracks, top and bottom.
        * The highest notes will go into top, the lowest into bottom.
        * This function is used to split piano songs into left-hand (bottom)
        * and right-hand (top) tracks.
        */
       public static List<MidiTrack> SplitTrack(MidiTrack track, int measurelen) {
           List<MidiNote> notes = track.Notes;
           int count = notes.Count;

           MidiTrack top = new MidiTrack(1);
           MidiTrack bottom = new MidiTrack(2);
           List<MidiTrack> result = new List<MidiTrack>(2);
           result.Add(top); result.Add(bottom);

           if (count == 0)
               return result;

           int prevhigh  = 76; /* E5, top of treble staff */
           int prevlow   = 45; /* A3, bottom of bass staff */
           int startindex = 0;

           for (MidiNote note : notes) {
               int high, low, highExact, lowExact;
               
               int number = note.Number;
               high = low = highExact = lowExact = number;

               while (notes[startindex].EndTime < note.StartTime) {
                   startindex++;
               }

               /* I've tried several algorithms for splitting a track : two,
                * and the one below seems to work the best:
                * - If this note is more than an octave from the high/low notes
                *   (that start exactly at this start time), choose the closest one.
                * - If this note is more than an octave from the high/low notes
                *   (in this note's time duration), choose the closest one.
                * - If the high and low notes (that start exactly at this starttime)
                *   are more than an octave apart, choose the closest note.
                * - If the high and low notes (that overlap this starttime)
                *   are more than an octave apart, choose the closest note.
                * - Else, look at the previous high/low notes that were more than an 
                *   octave apart.  Choose the closeset note.
                */
               FindHighLowNotes(notes, measurelen, startindex, note.StartTime, note.EndTime, 
                                ref high, ref low);
               FindExactHighLowNotes(notes, startindex, note.StartTime,
                                     ref highExact, ref lowExact);

               if (highExact - number > 12 || number - lowExact > 12) {
                   if (highExact - number <= number - lowExact) {
                       top.AddNote(note);
                   }
                   else {
                       bottom.AddNote(note);
                   }
               } 
               else if (high - number > 12 || number - low > 12) {
                   if (high - number <= number - low) {
                       top.AddNote(note);
                   }
                   else {
                       bottom.AddNote(note);
                   }
               } 
               else if (highExact - lowExact > 12) {
                   if (highExact - number <= number - lowExact) {
                       top.AddNote(note);
                   }
                   else {
                       bottom.AddNote(note);
                   }
               }
               else if (high - low > 12) {
                   if (high - number <= number - low) {
                       top.AddNote(note);
                   }
                   else {
                       bottom.AddNote(note);
                   }
               }
               else {
                   if (prevhigh - number <= number - prevlow) {
                       top.AddNote(note);
                   }
                   else {
                       bottom.AddNote(note);
                   }
               }

               /* The prevhigh/prevlow are set to the last high/low
                * that are more than an octave apart.
                */
               if (high - low > 12) {
                   prevhigh = high;
                   prevlow = low;
               }
           }

           top.Notes.Sort(track.Notes[0]);
           bottom.Notes.Sort(track.Notes[0]);

           return result;
       }


       /** Combine the notes : the given tracks into a single MidiTrack. 
        *  The individual tracks are already sorted.  To merge them, we
        *  use a mergesort-like algorithm.
        */
       public static MidiTrack CombineToSingleTrack(List<MidiTrack> tracks)
       {
           /* Add all notes into one track */
           MidiTrack result = new MidiTrack(1);

           if (tracks.Count == 0) {
               return result;
           }
           else if (tracks.Count == 1) {
               MidiTrack track = tracks[0];
               for (MidiNote note : track.Notes) {
                   result.AddNote(note);
               }
               return result;
           }

           int[] noteindex = new int[64];
           int[] notecount = new int[64];

           for (int tracknum = 0; tracknum < tracks.Count; tracknum++) {
               noteindex[tracknum] = 0;
               notecount[tracknum] = tracks[tracknum].Notes.Count;
           }
           MidiNote prevnote = null;
           while (true) {
               MidiNote lowestnote = null;
               int lowestTrack = -1;
               for (int tracknum = 0; tracknum < tracks.Count; tracknum++) {
                   MidiTrack track = tracks[tracknum];
                   if (noteindex[tracknum] >= notecount[tracknum]) {
                       continue;
                   }
                   MidiNote note = track.Notes[ noteindex[tracknum] ];
                   if (lowestnote == null) {
                       lowestnote = note;
                       lowestTrack = tracknum;
                   }
                   else if (note.StartTime < lowestnote.StartTime) {
                       lowestnote = note;
                       lowestTrack = tracknum;
                   }
                   else if (note.StartTime == lowestnote.StartTime && note.Number < lowestnote.Number) {
                       lowestnote = note;
                       lowestTrack = tracknum;
                   }
               }
               if (lowestnote == null) {
                   /* We've finished the merge */
                   break;
               }
               noteindex[lowestTrack]++;
               if ((prevnote != null) && (prevnote.StartTime == lowestnote.StartTime) &&
                   (prevnote.Number == lowestnote.Number) ) {

                   /* Don't add duplicate notes, with the same start time and number */        
                   if (lowestnote.Duration > prevnote.Duration) {
                       prevnote.Duration = lowestnote.Duration;
                   }
               }
               else {
                   result.AddNote(lowestnote);
                   prevnote = lowestnote;
               }
           }
       
           return result;
       }


       /** Combine the notes : all the tracks given into two MidiTracks,
        * and return them.
        * 
        * This function is intended for piano songs, when we want to display
        * a left-hand track and a right-hand track.  The lower notes go into 
        * the left-hand track, and the higher notes go into the right hand 
        * track.
        */
       public static List<MidiTrack> CombineToTwoTracks(List<MidiTrack> tracks, int measurelen)
       {
           MidiTrack single = CombineToSingleTrack(tracks);
           List<MidiTrack> result = SplitTrack(single, measurelen);

           List<MidiEvent> lyrics = new List<MidiEvent>();
           for (MidiTrack track : tracks) {
               if (track.Lyrics != null) {
                   lyrics.AddRange(track.Lyrics);
               }
           }
           if (lyrics.Count > 0) {
               lyrics.Sort(lyrics[0]);
               result[0].Lyrics = lyrics;
           } 

           return result;
       }


       /** Check that the MidiNote start times are : increasing order.
        * This is for debugging purposes.
        */
       private static void CheckStartTimes(List<MidiTrack> tracks) {
    	   for (MidiTrack track : tracks) {
               int prevtime = -1;
               for (MidiNote note : track.Notes) {
                   if (note.StartTime < prevtime) {
                       throw new System.ArgumentException("start times not : increasing order");
                   }
                   prevtime = note.StartTime;
               }
           }
       }


       /** : Midi Files, time is measured : pulses.  Notes that have
        * pulse times that are close together (like within 10 pulses)
        * will sound like they're the same chord.  We want to draw
        * these notes as a single chord, it makes the sheet music much
        * easier to read.  We don't want to draw notes that are close
        * together as two separate chords.
        *
        * The SymbolSpacing class only aligns notes that have exactly the same
        * start times.  Notes with slightly different start times will
        * appear : separate vertical columns.  This isn't what we want.
        * We want to align notes with approximately the same start times.
        * So, this function is used to assign the same starttime for notes
        * that are close together (timewise).
        */
       public static void
       RoundStartTimes(List<MidiTrack> tracks, int millisec, TimeSignature time) {
           /* Get all the starttimes : all tracks, : sorted order */
           List<int> starttimes = new List<int>();
           for (MidiTrack track : tracks) {
        	   for (MidiNote note : track.Notes) {
                   starttimes.Add( note.StartTime );
               }
           }
           starttimes.Sort();

           /* Notes within "millisec" milliseconds apart will be combined. */
           int interval = time.Quarter * millisec * 1000 / time.Tempo;

           /* If two starttimes are within interval millisec, make them the same */
           for (int i = 0; i < starttimes.Count - 1; i++) {
               if (starttimes[i+1] - starttimes[i] <= interval) {
                   starttimes[i+1] = starttimes[i];
               }
           }

           CheckStartTimes(tracks);

           /* Adjust the note starttimes, so that it matches one of the starttimes values */
           foreach (MidiTrack track : tracks) {
               int i = 0;

               for (MidiNote note : track.Notes) {
                   while (i < starttimes.Count &&
                          note.StartTime - interval > starttimes[i]) {
                       i++;
                   }

                   if (note.StartTime > starttimes[i] &&
                       note.StartTime - starttimes[i] <= interval) {

                       note.StartTime = starttimes[i];
                   }
               }
               track.Notes.Sort(track.Notes[0]);
           }
       }


       /** We want note durations to span up to the next note : general.
        * The sheet music looks nicer that way.  : contrast, sheet music
        * with lots of 16th/32nd notes separated by small rests doesn't
        * look as nice.  Having nice looking sheet music is more important
        * than faithfully representing the Midi File data.
        *
        * Therefore, this function rounds the duration of MidiNotes up to
        * the next note where possible.
        */
       public static void
       RoundDurations(List<MidiTrack> tracks, int quarternote) {

    	   for (MidiTrack track : tracks ) {
               MidiNote prevNote = null;
               for (int i = 0; i < track.Notes.Count-1; i++) {
                   MidiNote note1 = track.Notes[i];
                   if (prevNote == null) {
                       prevNote = note1;
                   }

                   /* Get the next note that has a different start time */
                   MidiNote note2 = note1;
                   for (int j = i+1; j < track.Notes.Count; j++) {
                       note2 = track.Notes[j];
                       if (note1.StartTime < note2.StartTime) {
                           break;
                       }
                   }
                   int maxduration = note2.StartTime - note1.StartTime;

                   int dur = 0;
                   if (quarternote <= maxduration)
                       dur = quarternote;
                   else if (quarternote/2 <= maxduration)
                       dur = quarternote/2;
                   else if (quarternote/3 <= maxduration)
                       dur = quarternote/3;
                   else if (quarternote/4 <= maxduration)
                       dur = quarternote/4;


                   if (dur < note1.Duration) {
                       dur = note1.Duration;
                   }

                   /* Special case: If the previous note's duration
                    * matches this note's duration, we can make a notepair.
                    * So don't expand the duration : that case.
                    */
                   if ((prevNote.StartTime + prevNote.Duration == note1.StartTime) &&
                       (prevNote.Duration == note1.Duration)) {

                       dur = note1.Duration;
                   }
                   note1.Duration = dur;
                   if (track.Notes[i+1].StartTime != note1.StartTime) {
                       prevNote = note1;
                   }
               }
           }
       }

       /** Split the given track into multiple tracks, separating each
        * channel into a separate track.
        */
       private static List<MidiTrack> 
       SplitChannels(MidiTrack origtrack, List<MidiEvent> events) {

           /* Find the instrument used for each channel */
           int[] channelInstruments = new int[16];
           for (MidiEvent mevent : events) {
               if (mevent.EventFlag == EventProgramChange) {
                   channelInstruments[Channel] = Instrument;
               }
           }
           channelInstruments[9] = 128; /* Channel 9 = Percussion */

           List<MidiTrack> result = new ArrayList<MidiTrack>();
           for (MidiNote note : origtrack.getNotes()) {
               boolean foundchannel = false;
               for (MidiTrack track : result) {
                   if (note.getChannel() == track.getNotes().get(0).getChannel()) {
                       foundchannel = true;
                       track.AddNote(note); 
                   }
               }
               if (!foundchannel) {
                   MidiTrack track = new MidiTrack(result.size() + 1);
                   track.AddNote(note);
                   track.setInstrument(channelInstruments[note.getChannel()]);
                   result.add(track);
               }
           }
           return result;
       }


       /** Guess the measure length.  We assume that the measure
        * length must be between 0.5 seconds and 4 seconds.
        * Take all the note start times that fall between 0.5 and 
        * 4 seconds, and return the starttimes.
        */
       public List<int> 
       GuessMeasureLength() {
           List<int> result = new List<int>();

           int pulses_per_second = (int) (1000000.0 / timesig.Tempo * timesig.Quarter);
           int minmeasure = pulses_per_second / 2;  /* The minimum measure length : pulses */
           int maxmeasure = pulses_per_second * 4;  /* The maximum measure length : pulses */

           /* Get the start time of the first note : the midi file. */
           int firstnote = timesig.Measure * 5;
           for (MidiTrack track : tracks) {
               if (firstnote > track.Notes[0].StartTime) {
                   firstnote = track.Notes[0].StartTime;
               }
           }

           /* interval = 0.06 seconds, converted into pulses */
           int interval = timesig.Quarter * 60000 / timesig.Tempo;

           for (MidiTrack track : tracks) {
               int prevtime = 0;
               for (MidiNote note : track.Notes) {
                   if (note.StartTime - prevtime <= interval)
                       continue;

                   prevtime = note.StartTime;

                   int time_from_firstnote = note.StartTime - firstnote;

                   /* Round the time down to a multiple of 4 */
                   time_from_firstnote = time_from_firstnote / 4 * 4;
                   if (time_from_firstnote < minmeasure)
                       continue;
                   if (time_from_firstnote > maxmeasure)
                       break;

                   if (!result.Contains(time_from_firstnote)) {
                       result.Add(time_from_firstnote);
                   }
               }
           }
           result.Sort();
           return result;
       }

       /** Return the last start time */
       public int EndTime() {
           int lastStart = 0;
           for (MidiTrack track : tracks) {
               if (track.Notes.Count == 0) {
                   continue;
               }
               int last = track.Notes[ track.Notes.Count-1 ].StartTime;
               lastStart = Math.Max(last, lastStart);
           }
           return lastStart;
       }

       /** Return true if this midi file has lyrics */
       public boolean HasLyrics() {
    	   for (MidiTrack track : tracks) {
               if (track.Lyrics != null) {
                   return true;
               }
           }
           return false;
       }

       public override String ToString() {
           String result = "Midi File tracks=" + tracks.Count + " quarter=" + quarternote + "\n";
           result += Time.ToString() + "\n";
           for(MidiTrack track : tracks) {
               result += track.ToString();
           }
           return result;
       }

       /* Command-line program to print out a parsed Midi file. Used for debugging.
        * To run:
        * - Change Main2 to Main
        * - csc MidiNote.cs MidiEvent.cs MidiTrack.cs MidiFileReader.cs MidiOptions.cs
        *   MidiFile.cs MidiException.cs TimeSignature.cs
        * - MidiFile.exe file.mid
        *
        */
       public static void Main2(string[] arg) {
           if (arg.Length == 0) {
               Console.WriteLine("Usage: MidiFile <filename>");
               return;
           }

           MidiFile f = new MidiFile(arg[0]);
           Console.Write(f.ToString());
       }

   }  /* End class MidiFile */

}
