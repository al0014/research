%
% JAVA SECTION: If required, change the name of the JAR file. That would
% also mean that you need to change the instance method.
%

if isempty(which('research.BrainComputerInterfaceGUI'))
    folder = fileparts(mfilename('fullpath'));
    javaaddpath([folder '\BCI.jar']);
end

% Instance method.
jFrame = research.BrainComputerInterfaceGUI();

jFrame.setVisible(true);
if isdeployed
    waitfor(jFrame);
end

%
% VARIABLES SECTION
%

nameOfModelWithoutExtension = 'BCI';
nameOfFirstSubjectSignalBlock = 'Scope';
nameOfSecondSubjectSignalBlock = 'Scope1';

%
% MATLAB SECTION
%

eegBasedBCI(nameOfModelWithoutExtension, nameOfFirstSubjectSignalBlock, nameOfSecondSubjectSignalBlock, jFrame);

% Note: use jFrame.dispose() to close jFrame, otherwise Matlab will exit!

