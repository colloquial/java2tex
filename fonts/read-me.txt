Installing Lucida fonts:

http://www.pctex.com/kb/74.html

[Bob's note:  I literally copied the contents using sudo
sudo cp -R lucimatx/texmf/* /usr/local/texlive/texmf-local 

Note: The directions below are for MacTeX or gwTeX installations under OS X. If your installation uses teTeX please see Installing under teTeX. Also consider joining the TeX on Mac OS X mailing list (http://www.apfelwiki.de/forum/viewtopic.php?t=348)].

1. Copy the contents of

       mtp2fonts/texmf
       lucimatx/texmf

into the directories:

For MacTeX:

   /usr/local/texlive/texmf-local 

For gwTeX:

   /usr/local/gwTeX/texmf.pkgs

2. Run the following command (you will have to enter an administrator password to use sudo)

  sudo texhash

3. Add the map file.

For MathTimePro II, run

  sudo updmap-sys --enable Map=mtpro2.map 

For Lucida, run

  sudo updmap-sys --enable Map=lucida.map 

4. For MathTimePro II, it is recommended to remove the belleek Times fonts

  sudo updmap-sys --disable Map=belleek.map 

(Thanks to Herb Schulz of the TeXShop wiki (http://www.apfelwiki.de/forum/viewforum.php?f=6)) 
