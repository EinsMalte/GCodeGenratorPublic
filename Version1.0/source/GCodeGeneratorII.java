import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class GCodeGeneratorII extends PApplet {

//GCodegenrator with UI

String[] words;
public void setup() {
  
  frameRate(1000);
  cl();
}

int lang = 1; //0 Deutsch 1 English

boolean visualize = false;
PImage img = null;
boolean mb = false;
boolean loadImage = false;
float groovesize = 1;
boolean gss = false; //If groovesize is selected
boolean calc = false;
float progress = 0;
public void draw() {
  background(0);
  if (img == null) {
    
    noStroke();
    background(255);
    stroke(230);
    fill(240);
    rect(width/2-width/10, height/2-height/20, width/5, height/10, 10);
    fill(0);
    textAlign(CENTER, CENTER);
    textSize(height/20);
    text(words[1], width/2, height/2);
    textSize(height/50);
    textAlign(CENTER, TOP);
    text(words[2], width/2, height/2+height/20);
    fill(244);
    noStroke();
    rect(width/2-width/10, height/1.5f-height/20, width/5, height/10, 10);
    fill(0);
    text(words[6],width/2,height/1.5f);
    if (mouseX > width/2-width/10 && mouseX < width/2+width/10 && mouseY > height/2-height/10 && mouseY < height/2+height/10) {
      cursor(HAND);
      if (mousePressed && !mb) {
        loadImage = true;
        mb = true;
      }
    } else if(mouseX > width/2-width/10 && mouseX < width/1.5f+width/10 && mouseY > height/2-height/10 && mouseY < height/1.5f+height/10) {
      cursor(HAND);
      if(mousePressed && !mb) {
        if(lang == 1) lang = 0;
        else lang = 1;
        cl();
        mb = true;
      }
    } else cursor(ARROW);
  } else {
    boolean c = false; //If cursor is not arrow
    strokeWeight(1);
    background(255);
    image(img, 0, 0);
    stroke(0);
    fill(0);
    line(600, 0, 600, height);
    textAlign(CENTER, CENTER);
    textSize(width/50);
    text(words[3], 700, 10);
    textSize(width/60);
    text(words[4] + groovesize/10 + "cm", 700, 40);
    strokeWeight(5);
    stroke(200);
    line(620, 60, 780, 60);
    fill(150);
    noStroke();
    ellipse(620+map(groovesize, 1, 5, 0, 160), 60, 20, 20);
    fill(240);
    rect(620, 520, 160, 60, 10);
    if (visualize) fill(0, 255, 0);
    else fill(244);
    rect(620, 80, 160, 40, 10);
    fill(0);
    text(words[5], 700, 100);
    if (mouseX > 620 && mouseX < 780 && mouseY > 80 && mouseY < 120) {
      cursor(HAND);
      c = true;
      if(mousePressed && !mb) {
        mb = true;
        visualize = !visualize;
      }
    }
    if (!calc) {
      fill(0);
      textAlign(CENTER, CENTER);
      text("Los!", 700, 550);
    } else {
      fill(0, 255, 0);
      rect(620, 520, map(progress, 0, 100, 0, 160), 60, 10);
      fill(0);
      textAlign(CENTER, CENTER);
      textSize(width/50);
      text(round(progress) + "%", 700, 550);
    }
    if (calc)
      if (bpx.size() > 20 && calc) for (int i = 0; i < 10; i++) calculate(); 
      else calculate();
    if (mouseX > 620 && mouseX < 780 && mouseY > 520 && mouseY < 580) {
      cursor(HAND);
      c = true;
      if (mousePressed && !mb) {
        mb = true;
        calc = true;
        prep();
      }
    }
    if (dist(mouseX, mouseY, 620+map(groovesize, 1, 5, 0, 160), 60) < 10 && mouseX > 620 && mouseX < 780 && !mb) {
      cursor(HAND);
      c = true;
      gss = true;
      mb = true;
    }
    if (gss) 
      if (mousePressed) {
        groovesize = map(mouseX, 620, 780, 1, 5);
        groovesize = groovesize-groovesize%0.1f;
        if (mouseX < 620) groovesize = 1;
        if (mouseX > 780) groovesize = 5;
      }
    if (!c) cursor(ARROW);
  }
  fill(0);
  textAlign(RIGHT, BOTTOM);
  textSize(width/70);
  text("©Malte Haan", width, height);
}

public void fileSelected(File selection) {
  if (selection == null) {
    println("Window was closed or the user hit cancel.");
  } else {
    println("User selected " + selection.getAbsolutePath());

    String s = selection.getAbsolutePath();
    if (s.indexOf(".png") > 0) img = loadImage(selection.getAbsolutePath());
    if (img.width > 600 || img.height > 600) {
      img = null;
      println("too big");
    }
  }
}

public void mouseReleased() {
  if (!calc)
    mb = false;
  if (loadImage) selectInput(words[0], "fileSelected");
  loadImage = false;
  gss = false;
}

public void cl() {
  if(lang == 0) words = loadStrings("Deutsch.txt");
  if(lang == 1) words = loadStrings("English.txt");
}

StringList gcode;
IntList bpx;
IntList bpy;
int x;
int y;
int s = 0;
public void prep() {
  gcode = new StringList();
  bpx = new IntList();
  bpy = new IntList();
  for (int xx = 0; xx < img.width; xx++) {
    for (int yy = 0; yy < img.height; yy++) {
      if (brightness(img.pixels[xx+yy*img.width]) < 255/2) {
        bpx.append(xx);
        bpy.append(yy);
      }
    }
  }
  x = 0;
  y = 0;
  gcode.append("G0 Z10");
  gcode.append("G0 X0 Y0 Z0");
  s = bpx.size();
  println(s);
}

public void calculate() {
  strokeWeight(1);
  progress = map(s-bpx.size(), 0, s, 0, 100);
  float bd = 9999;
  int bx = 0;
  int by = 0;
  int bi = 0;
  int l = 0;
  stroke(255, 0, 0);
  fill(255, 0, 0);
  for (int i = 0; i < bpx.size(); i++) {
    l++;
    if (i%100 == 0 && visualize) {
      ellipse(bpx.get(i), bpy.get(i), 5, 5);
    }
    if (dist(bpx.get(i), bpy.get(i), x, y) < bd) {
      bx = bpx.get(i);
      by = bpy.get(i);
      bi = i;
      bd = dist(bpx.get(i), bpy.get(i), x, y);
    }
  }
  println(l, bpx.size(), bd);
  if (l == 0) {
    println("finished");
    selectOutput("Select a file to write to:", "OutputSelected");
    
    calc = false;
    mb = false;
  } else {
    if (dist(x, y, bx, by) < 2) {
      gcode.append("G1 X" + bx*0.01f + " Y" + by*0.01f + " Z" + (10-groovesize));
    } else { 
      gcode.append("G1 X" + x*0.01f + " Y" + y*0.01f + " Z11");
      gcode.append("G0 X" + bx*0.01f + " Y" + by*0.01f + " Z11");
      gcode.append("G1 X" + bx*0.01f + " Y" + by*0.01f + " Z" + (10-groovesize));
    }
    x = bx;
    y = by;
    bpx.remove(bi);
    bpy.remove(bi);
    fill(0, 255, 0);
    stroke(0, 255, 0);
    ellipse(x, y, 20, 20);
  }
}

public void OutputSelected(File selection) {
  if (selection == null) {
    println("Window was closed or the user hit cancel.");
  } else {
    println("User selected " + selection.getAbsolutePath());

    String s = selection.getAbsolutePath();
    saveStrings(s, gcode.array());
  }
  gcode = null;
}
  public void settings() {  size(800, 600); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "GCodeGeneratorII" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
