// SCOUT GUI
// (c) Copyright 2013-2014, BSI Business Systems Integration AG

Scout.DesktopToolButton = function (scout, $desktop, toolButtons) {
  // create container
  var $desktopTools = $desktop.appendDiv('DesktopTools');

  // create tool-items
  for (var i = 0; i < toolButtons.length; i++) {
    var state = toolButtons[i].state || '',
      icon = toolButtons[i].icon || '',
      shortcut = toolButtons[i].shortcut || '';

    var $tool = $desktopTools
      .appendDiv(toolButtons[i].id, 'tool-item ' + state, toolButtons[i].label)
      .attr('data-icon', icon).attr('data-shortcut', shortcut);

    if (!$tool.hasClass('disabled')) {
      $tool.on('click', '', clickTool);
    }
  }

  // create container for dialogs
  $desktopTools.appendDiv('DesktopDialogs');

  // set this for later usage
  this.$div = $desktopTools;

  // named event funktions
  function clickTool (event) {
    var $clicked = $(this);

    $('.tool-open').animateAVCSD('width', 0, $.removeThis, null, 500);

    if ($clicked.hasClass("selected")) {
      $clicked.removeClass("selected");
    } else {
      $clicked.selectOne();
      $('#DesktopTools').beforeDiv('', 'tool-open')
        .animateAVCSD('width', 300, null, null, 500);
    }
  }
};
