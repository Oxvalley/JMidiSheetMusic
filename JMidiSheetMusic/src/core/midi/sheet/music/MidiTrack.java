package core.midi.sheet.music;

/*
 * C# original Copyright (c) 2007-2012 Madhav Vaidyanathan
 * Java port   Copyright(c) 2012 Lars Svensson
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */

import java.util.ArrayList;
import java.util.List;

/**
 * @class MidiTrack The MidiTrack takes as input the raw MidiEvents for the
 *        track, and gets: - The list of midi notes in the track. - The first
 *        instrument used in the track.
 * 
 *        For each NoteOn event in the midi file, a new MidiNote is created and
 *        added to the track, using the AddNote() method.
 * 
 *        The NoteOff() method is called when a NoteOff event is encountered, in
 *        order to update the duration of the MidiNote.
 */
public class MidiTrack
{
   private int tracknum;
   /** The track number */
   private List<MidiNote> notes;
   /** List of Midi notes */
   private int instrument;
   /** Instrument for this track */
   private List<MidiEvent> lyrics;

   /** The lyrics in this track */

   /** Create an empty MidiTrack. Used by the Clone method */
   public MidiTrack(int tracknum)
   {
      this.tracknum = tracknum;
      notes = new ArrayList<MidiNote>();
      instrument = 0;
   }

   /**
    * Create a MidiTrack based on the Midi events. Extract the NoteOn/NoteOff
    * events to gather the list of MidiNotes.
    */
   public MidiTrack(List<MidiEvent> events, int tracknum)
   {
      this.tracknum = tracknum;
      notes = new ArrayList<MidiNote>();
      instrument = 0;

      for (MidiEvent mevent : events)
      {
         processEvent(mevent);
      }
      if (notes.size() > 0 && notes.get(0).getChannel() == 9)
      {
         instrument = 128; /* Percussion */
      }
      // int lyriccount = 0;
      // if (lyrics != null)
      // {
      // lyriccount = lyrics.size();
      // }
   }

   private void processEvent(MidiEvent mevent)
   {
      if (mevent.EventFlag == MidiFile.EventNoteOn && mevent.Velocity > 0)
      {
         MidiNote note = new MidiNote(mevent.StartTime, mevent.Channel,
               mevent.Notenumber, 0);
         AddNote(note);
      }
      else if (mevent.EventFlag == MidiFile.EventNoteOn && mevent.Velocity == 0)
      {
         NoteOff(mevent.Channel, mevent.Notenumber, mevent.StartTime);
      }
      else if (mevent.EventFlag == MidiFile.EventNoteOff)
      {
         NoteOff(mevent.Channel, mevent.Notenumber, mevent.StartTime);
      }
      else if (mevent.EventFlag == MidiFile.EventProgramChange)
      {
         instrument = mevent.Instrument;
      }
      else if (mevent.Metaevent == MidiFile.MetaEventLyric)
      {
         if (lyrics == null)
         {
            lyrics = new ArrayList<MidiEvent>();
         }
         lyrics.add(mevent);
      }
   }

   public MidiTrack(MidiEvent midiEvent, int tracknum2)
   {
      this.tracknum = tracknum2;
      notes = new ArrayList<MidiNote>();
      instrument = 0;

      processEvent(midiEvent);
      if (notes.size() > 0 && notes.get(0).getChannel() == 9)
      {
         instrument = 128; /* Percussion */
      }
      // int lyriccount = 0;
      // if (lyrics != null)
      // {
      // lyriccount = lyrics.size();
      // }
   }

   public int getNumber()
   {
      return tracknum;
   }

   public List<MidiNote> getNotes()
   {
      return notes;
   }

   public int getInstrument()
   {
      return instrument;
   }

   public void setInstrument(int value)
   {
      instrument = value;
   }

   public String getInstrumentName()
   {
      if (instrument >= 0 && instrument <= 128)
         return MidiFile.Instruments[instrument];
      else
         return "";

   }

   public List<MidiEvent> getLyrics()
   {
      return lyrics;
   }

   public void setLyrics(List<MidiEvent> value)
   {
      lyrics = value;
   }

   /** Add a MidiNote to this track. This is called for each NoteOn event */
   public void AddNote(MidiNote m)
   {
      notes.add(m);
   }

   /**
    * A NoteOff event occured. Find the MidiNote of the corresponding NoteOn
    * event, and update the duration of the MidiNote.
    */
   public void NoteOff(int channel, int notenumber, int endtime)
   {
      for (int i = notes.size() - 1; i >= 0; i--)
      {
         MidiNote note = notes.get(i);
         if (note.getChannel() == channel && note.getNoteNumber() == notenumber
               && note.getDuration() == 0)
         {
            note.NoteOff(endtime);
            return;
         }
      }
   }

   /** Return a deep copy clone of this MidiTrack. */
   public MidiTrack Clone()
   {
      MidiTrack track = new MidiTrack(getNumber());
      track.instrument = instrument;
      for (MidiNote note : notes)
      {
         track.notes.add(note.Clone());
      }
      if (lyrics != null)
      {
         track.lyrics = new ArrayList<MidiEvent>();
         for (MidiEvent ev : lyrics)
         {
            track.lyrics.add(ev);
         }
      }
      return track;
   }

   @Override
   public String toString()
   {
      String result = "Track number=" + tracknum + " instrument=" + instrument
            + "\n";
      for (MidiNote n : notes)
      {
         result = result + n + "\n";
      }
      result += "End Track\n";
      return result;
   }

}
