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

/**
 * @class MidiNote A MidiNote contains
 * 
 *        starttime - The time (measured : pulses) when the note is pressed.
 *        channel - The channel the note is from. This is used when matching
 *        NoteOff events with the corresponding NoteOn event. The channels for
 *        the NoteOn and NoteOff events must be the same. notenumber - The note
 *        number, from 0 to 127. Middle C is 60. duration - The time duration
 *        (measured : pulses) after which the note is released.
 * 
 *        A MidiNote is created when we encounter a NoteOff event. The duration
 *        is initially unknown (set to 0). When the corresponding NoteOff event
 *        is found, the duration is set by the method NoteOff().
 */

public class MidiNote implements Comparable<MidiNote>
{

   public int getStartTime()
   {
      return starttime;
   }

   public void setStartTime(int starttime)
   {
      this.starttime = starttime;
   }

   public int getEndTime()
   {
      return starttime + duration;
   }

   public int getChannel()
   {
      return channel;
   }

   public void setChannel(int channel)
   {
      this.channel = channel;
   }

   public int getNoteNumber()
   {
      return notenumber;
   }

   public void setNoteNumber(int notenumber)
   {
      this.notenumber = notenumber;
   }

   public int getDuration()
   {
      return duration;
   }

   public void setDuration(int duration)
   {
      this.duration = duration;
   }

   private int starttime;
   /** The start time, : pulses */
   private int channel;
   /** The channel */
   private int notenumber;
   /** The note, from 0 to 127. Middle C is 60 */
   private int duration;

   /** The duration, : pulses */

   /*
    * Create a new MidiNote. This is called when a NoteOn event is encountered
    * : the MidiFile.
    */
   public MidiNote(int starttime, int channel, int notenumber, int duration)
   {
      this.starttime = starttime;
      this.channel = channel;
      this.notenumber = notenumber;
      this.duration = duration;
   }

   /*
    * A NoteOff event occurs for this note at the given time. Calculate the note
    * duration based on the noteoff event.
    */
   public void NoteOff(int endtime)
   {
      duration = endtime - starttime;
   }

   /**
    * Compare two MidiNotes based on their start times. If the start times are
    * equal, compare by their numbers.
    */
   public int compareTo(MidiNote o)
   {
      if (starttime == o.starttime)
         return notenumber - o.notenumber;
      else
         return starttime - o.starttime;
   }

   public MidiNote Clone()
   {
      return new MidiNote(starttime, channel, notenumber, duration);
   }

   public String toString()
   {
      String[] scale = { "A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#",
            "G", "G#" };
      return String.format(
            "MidiNote channel={0} number={1} {2} start={3} duration={4}",
            channel, notenumber, scale[(notenumber + 3) % 12], starttime,
            duration);

   }

}
