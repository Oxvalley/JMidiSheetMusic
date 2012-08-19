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

/**
 * @class MidiEvent A MidiEvent represents a single event (such as EventNoteOn)
 *        in the Midi file. It includes the delta time of the event.
 */
public class MidiEvent implements Comparable<MidiEvent>
{

   public int DeltaTime;
   /** The time between the previous event and this on */
   public int StartTime;
   /** The absolute time this event occurs */
   public boolean HasEventflag;
   /** False if this is using the previous eventflag */
   public int EventFlag;
   /** NoteOn, NoteOff, etc. Full list is in class MidiFile */
   public int Channel;
   /** The channel this event occurs on */

   public int Notenumber;
   /** The note number */
   public int Velocity;
   /** The volume of the note */
   public int Instrument;
   /** The instrument */
   public int KeyPressure;
   /** The key pressure */
   public int ChanPressure;
   /** The channel pressure */
   public int ControlNum;
   /** The controller number */
   public int ControlValue;
   /** The controller value */
   public int PitchBend;
   /** The pitch bend value */
   public int Numerator;
   /** The numerator, for TimeSignature meta events */
   public int Denominator;
   /** The denominator, for TimeSignature meta events */
   public int Tempo;
   /** The tempo, for Tempo meta events */
   public int Metaevent;
   /** The metaevent, used if eventflag is MetaEvent */
   public int Metalength;
   /** The metaevent length */
   public char[] Value;

   /** The raw int value, for Sysex and meta events */

   public MidiEvent()
   {
   }

   /** Return a copy of this event */
   public MidiEvent Clone()
   {
      MidiEvent mevent = new MidiEvent();
      mevent.DeltaTime = DeltaTime;
      mevent.StartTime = StartTime;
      mevent.HasEventflag = HasEventflag;
      mevent.EventFlag = EventFlag;
      mevent.Channel = Channel;
      mevent.Notenumber = Notenumber;
      mevent.Velocity = Velocity;
      mevent.Instrument = Instrument;
      mevent.KeyPressure = KeyPressure;
      mevent.ChanPressure = ChanPressure;
      mevent.ControlNum = ControlNum;
      mevent.ControlValue = ControlValue;
      mevent.PitchBend = PitchBend;
      mevent.Numerator = Numerator;
      mevent.Denominator = Denominator;
      mevent.Tempo = Tempo;
      mevent.Metaevent = Metaevent;
      mevent.Metalength = Metalength;
      mevent.Value = Value;
      return mevent;
   }

   @Override
   public int compareTo(MidiEvent y)
   {
      if (StartTime == y.StartTime)
      {
         if (EventFlag == y.EventFlag)
         {
            return Notenumber - y.Notenumber;
         }
         else
         {
            return EventFlag - y.EventFlag;
         }
      }
      else
      {
         return StartTime - y.StartTime;
      }
   }

} /* End namespace MidiSheetMusic */
