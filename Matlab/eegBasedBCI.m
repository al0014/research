function returnValue = eegBasedBCI(nameOfModelWithoutExtension, nameOfFirstSubjectSignalBlock, nameOfSecondSubjectSignalBlock, jFrame)
    % Name of the model that is being controlled.
    modelName = nameOfModelWithoutExtension;
    
    if ~verifyModelExistence(modelName)
        errordlg(sprintf('The model %s.mdl cannot be found.', modelName), 'File Verification', 'modal');
        return
    end
    
    nargoutchk(0, 1);

    fig = findall(0, 'Tag', mfilename);
    
    if isempty(fig)
        fig = localCreateUI(modelName, nameOfFirstSubjectSignalBlock, nameOfSecondSubjectSignalBlock, jFrame);
    else
        figure(fig);
    end

    if nargout > 0
        returnValue{1} = fig;
    end
end

function modelExists = verifyModelExistence(modelName)
    result = exist(modelName,'file');
    
    if result == 4
        modelExists = true;
    else
        modelExists = false;
    end
end

function fig = localCreateUI(modelName, nameOfFirstSubjectSignalBlock, nameOfSecondSubjectSignalBlock, jFrame)
    try
        % Create visualisation figure and axes.
        fig = figure('Tag', mfilename,...
            'Toolbar', 'none',...
            'MenuBar', 'none',...
            'IntegerHandle', 'off',...
            'Units', 'normalized',...
            'Resize', 'off',...
            'NumberTitle', 'off',...
            'HandleVisibility', 'callback',...
            'Name', 'EEG-based BCI',...
            'CloseRequestFcn', @localCloseRequestFcn,...
            'Visible', 'off');

        ax = axes('Parent', fig,...
            'HandleVisibility', 'callback',...
            'Unit', 'normalized',...
            'OuterPosition', [0.25 0.1 0.75 0.8],...
            'Xlim', [0 10],...
            'YLim', [-3 3],...
            'Tag', 'axes');

        xlabel(ax,'Time');
        ylabel(ax,'Signal Value');
        grid(ax,'on');
        box(ax,'on');

        % Create a panel for operations that can be performed.
        panel = uipanel('Parent', fig,...
            'Units', 'normalized',...
            'Position', [0.02 0.7 0.21 0.27],...
            'Title', 'Operations',...
            'BackgroundColor', get(fig, 'Color'),...
            'HandleVisibility', 'callback',...
            'Tag', 'operationsPanel');

        strings = {'Start', 'Stop'};
        positions = [0.6 0.2];
        tags = {'startpb','stoppb'};
        callbacks = {@localStartPressed, @localStopPressed};
        enabled = {'on','off'};

        for pointer = 1:length(strings)
            uicontrol('Parent', panel,...
                'Style', 'pushbutton',...
                'Units', 'normalized',...
                'Position', [0.15 positions(pointer) 0.7 0.2],...
                'BackgroundColor', get(fig, 'Color'),...
                'String', strings{pointer},...
                'Enable', enabled{pointer},...
                'Callback', callbacks{pointer},...
                'HandleVisibility', 'callback',...
                'Tag', tags{pointer});
        end

        try
            ad = createApplicationData(modelName, nameOfFirstSubjectSignalBlock, nameOfSecondSubjectSignalBlock);

            amountOfLines = length(ad.viewing);
            lines = nan(1, amountOfLines);
            colourOrder = get(ax, 'ColorOrder');

            for pointer = 1:amountOfLines
                lines(pointer) = line('Parent', ax,...
                    'XData', [],...
                    'YData', [],...
                    'Color', colourOrder(mod(pointer-1, size(colourOrder, 1))+1,:),...
                    'EraseMode', 'xor',...
                    'Tag', sprintf('signalLine%d', pointer));
            end

            ad.lineHandles = lines;

        catch err %#ok

        end

        helpMenu = uimenu('Parent', fig,...
            'Label', 'Help',...
            'Tag', 'helpmenu');

        labels = {'About'};
        tags = {'aboutpd'};
        callbacks = {@localAboutPulldown};
        
        for pointer = 1:length(labels)
            uimenu('Parent', helpMenu,...
                'Label', labels{pointer},...
                'Callback', callbacks{pointer},...
                'Tag', tags{pointer});
        end

        ad.handles = guihandles(fig);
        
        ad.jFrame = jFrame;

        guidata(fig, ad);

        movegui(fig, 'center')

        set(fig, 'Visible', 'on');
    catch err
        if exist('fig', 'var') && ~isempty(fig) && ishandle(fig)
            delete(fig);
        end
        
        close_system(modelName, 0)
        
        estr = sprintf('%s\n%s\n\n',...
            'The UI could not be created.',...
            'The specific error was:',...
            err.message);
        
        errordlg(estr,'EEG-based BCI','modal');
    end
end

function ad = createApplicationData(modelName, nameOfFirstSubjectSignalBlock, nameOfSecondSubjectSignalBlock)
    if ~modelIsLoaded(modelName)
        load_system(modelName);
    end
    
    ad.modelName = modelName;
    
    ad.viewing = struct(...
        'blockName','',...
        'blockHandle',[],...
        'blockEvent','',...
        'blockFcn',[]);
    
    ad.viewing(1).blockName = sprintf('%s/%s', ad.modelName, nameOfFirstSubjectSignalBlock);
    ad.viewing(2).blockName = sprintf('%s/%s', ad.modelName, nameOfSecondSubjectSignalBlock);
    
    ad.viewing(1).blockHandle = get_param(ad.viewing(1).blockName,'Handle');
    ad.viewing(2).blockHandle = get_param(ad.viewing(2).blockName,'Handle');
    
    ad.viewing(1).blockEvent = 'PostOutputs';
    ad.viewing(2).blockEvent = 'PostOutputs';
    
    ad.viewing(1).blockFcn = @firstSubjectEventListener;
    ad.viewing(2).blockFcn = @secondSubjectEventListener;

    ad.originalStopTime = get_param(ad.modelName,'Stoptime');
    ad.originalMode =  get_param(ad.modelName,'SimulationMode');
    ad.originalStartFcn = get_param(ad.modelName,'StartFcn');

    ad.modelAlreadyBuilt = false;
end

function modelLoaded = modelIsLoaded(modelName)
    try
        modelLoaded = ~isempty(find_system('Type', 'block_diagram', 'Name', modelName));
    catch  
        modelLoaded = false;
    end
end

function localStartPressed(hObject,eventdata) %#ok
    ad = guidata(hObject);

    if ~modelIsLoaded(ad.modelName)
        load_system(ad.modelName);
    end

    set(ad.handles.startpb,'Enable','off');
    set(ad.handles.stoppb,'Enable','on');

    for pointer = 1:length(ad.lineHandles)
        set(ad.lineHandles(pointer),...
            'XData', [],...
            'YData', []);
    end

    set_param(ad.modelName,'StopTime','inf');
    set_param(ad.modelName,'SimulationMode','normal');
    set_param(ad.modelName,'StartFcn','localAddEventListener');
    set_param(ad.modelName,'SimulationCommand','start');
end

function localStopPressed(hObject,eventdata) %#ok
    ad = guidata(hObject);

    set_param(ad.modelName,'SimulationCommand','stop');

    set_param(ad.modelName,'Stoptime',ad.originalStopTime);
    set_param(ad.modelName,'SimulationMode',ad.originalMode);

    set(ad.handles.startpb,'Enable','on');
    set(ad.handles.stoppb,'Enable','off');

    localRemoveEventListener;
end

function localCloseRequestFcn(hObject,eventdata) %#ok
    ad = guidata(hObject);

    if modelIsLoaded(ad.modelName)
        switch get_param(ad.modelName, 'SimulationStatus');
            case 'stopped'
                close_system(ad.modelName, 0);
                delete(gcbo);
            otherwise
                errordlg('The model must be stopped before the UI is closed.',...
                    'EEG-based BCI','modal');
        end
    else
        delete(gcbo);
    end
end

function localAddEventListener
    ad = guidata(gcbo);

    if ~isempty(ad.originalStartFcn)
        evalin('Base', ad.originalStartFcn);
    end

    ad.eventHandle = cell(1, length(ad.viewing));
    for pointer = 1:length(ad.viewing)
        ad.eventHandle{pointer} = ...
            add_exec_event_listener(ad.viewing(pointer).blockName,...
            ad.viewing(pointer).blockEvent, ad.viewing(pointer).blockFcn);
    end

    guidata(gcbo,ad);
end

function firstSubjectEventListener(block, eventdata) %#ok
    fig = findall(0, 'tag', mfilename);
    ad = guidata(fig);

%     thisLineHandle = ...
%         ad.lineHandles([ad.viewing.blockHandle] == block.BlockHandle);
% 
%     xdata = get(thisLineHandle,'XData');
%     ydata = get(thisLineHandle,'YData');
   
%     sTime = block.CurrentTime;
    data = block.InputPort(1).Data;
    
    ad.jFrame.updateFirstSubjectSignal(data);
    
%     if length(xdata) < 1001
%         newXData = [xdata sTime];
%         newYData = [ydata data];
%     else
%         newXData = [xdata(2:end) sTime];
%         newYData = [ydata(2:end) data];
%     end
% 
%     set(thisLineHandle,...
%         'XData', newXData,...
%         'YData', newYData);
% 
%     newXLim = [max(0,sTime-10) max(10,sTime)];
%     set(ad.handles.plotAxes, 'Xlim', newXLim);
end

function secondSubjectEventListener(block, eventdata) %#ok
    fig = findall(0, 'tag', mfilename);
    ad = guidata(fig);

%     thisLineHandle = ...
%         ad.lineHandles([ad.viewing.blockHandle] == block.BlockHandle);
% 
%     xdata = get(thisLineHandle,'XData');
%     ydata = get(thisLineHandle,'YData');
   
%     sTime = block.CurrentTime;
    data = block.InputPort(1).Data;
    
    ad.jFrame.updateSecondSubjectSignal(data);
    
%     if length(xdata) < 1001
%         newXData = [xdata sTime];
%         newYData = [ydata data];
%     else
%         newXData = [xdata(2:end) sTime];
%         newYData = [ydata(2:end) data];
%     end
% 
%     set(thisLineHandle,...
%         'XData', newXData,...
%         'YData', newYData);
% 
%     newXLim = [max(0,sTime-10) max(10,sTime)];
%     set(ad.handles.plotAxes, 'Xlim', newXLim);
end

function localRemoveEventListener
    ad = guidata(gcbo);

    set_param(ad.modelName,'StartFcn',ad.originalStartFcn);

    for pointer = 1:length(ad.eventHandle)
        if ishandle(ad.eventHandle{pointer})
            delete(ad.eventHandle{pointer});
        end
    end
    
    ad = rmfield(ad,'eventHandle');
    
    guidata(gcbo,ad);
end

function localAboutPulldown(hObject,eventdata) %#ok
    str = {[mfilename,' was written by Abdullah Garcia.'];...
        'Contact: abdullah.garcia@gmail.com';...
        ' ';...
        'Version 1.0'};
    
    msgbox(str,'About', 'Help', 'modal');
end





