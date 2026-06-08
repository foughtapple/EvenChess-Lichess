Option Explicit

Dim fso
Dim shell
Dim scriptDir
Dim launcherPath
Dim command

Set fso = CreateObject("Scripting.FileSystemObject")
Set shell = CreateObject("WScript.Shell")

scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
launcherPath = fso.BuildPath(scriptDir, "evenchess-testground.ps1")
command = "powershell.exe -NoProfile -ExecutionPolicy Bypass -File " & Quote(launcherPath)

shell.Run command, 0, False

Function Quote(value)
    Quote = Chr(34) & value & Chr(34)
End Function
