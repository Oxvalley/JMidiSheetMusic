package core.midi.sheet.music;

import java.awt.Color;

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



      /** @class MidiOptions
       *
       * The MidiOptions class contains the available options for
       * modifying the sheet music and sound.  These options are
       * collected from the menu/dialog settings, and then are passed
       * to the SheetMusic and MidiPlayer classes.
       */
      public class MidiOptions {

          // The possible values for showNoteLetters
          public static int NoteNameNone           = 0;
          public static int NoteNameLetter         = 1;
          public static int NoteNameFixedDoReMi    = 2;
          public static int NoteNameMovableDoReMi  = 3;
          public static int NoteNameFixedNumber    = 4;
          public static int NoteNameMovableNumber  = 5;

          // Sheet Music Options
          public boolean[] tracks;         /** Which tracks to display (true = display) */
          public boolean scrollVert;       /** Whether to scroll vertically or horizontally */
          public boolean largeNoteSize;    /** Display large or small note sizes */
          public boolean twoStaffs;        /** Combine tracks into two staffs ? */
          public int showNoteLetters;     /** Show the name (A, A#, etc) next to the notes */
          public boolean showLyrics;       /** Show the lyrics under each note */
          public boolean showMeasures;     /** Show the measure numbers for each staff */
          public int shifttime;         /** Shift note starttimes by the given amount */
          public int transpose;         /** Shift note key up/down by given amount */
          public int key;               /** Use the given KeySignature (notescale) */
          public TimeSignature time;    /** Use the given time signature */
          public int combineInterval;   /** Combine notes within given time interval (msec) */
          public Color[] colors;        /** The note colors to use */
          public Color shadeColor;      /** The color to use for shading. */
          public Color shade2Color;     /** The color to use for shading the left hand piano */

          // Sound options
          public boolean []mute;            /** Which tracks to mute (true = mute) */
          public int tempo;              /** The tempo, in microseconds per quarter note */
          public int pauseTime;          /** Start the midi music at the given pause time */
          public int[] instruments;      /** The instruments to use per track */
          public boolean useDefaultInstruments;  /** If true, don't change instruments */
          public boolean playMeasuresInLoop;     /** Play the selected measures in a loop */
          public int playMeasuresInLoopStart; /** Start measure to play in loop */
          public int playMeasuresInLoopEnd;   /** End measure to play in loop */

          public MidiOptions(MidiFile midifile) {
              int numtracks = midifile.getTracks().size();
              tracks = new boolean[numtracks];
              mute =  new boolean[numtracks];
              instruments = new int[numtracks];
              for (int i = 0; i < tracks.length; i++) {
                  tracks[i] = true;
                  mute[i] = false;
                  instruments[i] = midifile.getTracks().get(i).getInstrument();
                  if (midifile.getTracks().get(i).getInstrumentName().equals("Percussion")) {
                      tracks[i] = false;
                  }
              } 
              useDefaultInstruments = true;
              scrollVert = true;
              largeNoteSize = false;
              if (tracks.length == 1) {
                  twoStaffs = true;
              }
              else {
                  twoStaffs = false;
              }
              showNoteLetters = NoteNameNone;
              showLyrics = true;
              showMeasures = false;
              shifttime = 0;
              transpose = 0;
              key = -1;
              time = midifile.getTime();
              colors = null;
              shadeColor = new Color(210, 205, 220);
              shade2Color = new Color(80, 100, 250);
              combineInterval = 40;
              tempo = midifile.getTime().getTempo();
              pauseTime = 0;
              playMeasuresInLoop = false; 
              playMeasuresInLoopStart = 0;
              playMeasuresInLoopEnd = midifile.EndTime() / midifile.getTime().getMeasure();
          }
      
   }

