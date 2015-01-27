scout.CalendarLayout = function(calendar) {
  scout.CalendarLayout.parent.call(this);
  this.calendar = calendar;
  this.invalidateOnResize = true;
};
scout.inherits(scout.CalendarLayout, scout.AbstractLayout);

scout.CalendarLayout.prototype.layout = function($container) {
  var height = 0;

  height += $container.cssMarginTop() + $container.cssMarginBottom();
  $container.css('height', 'calc(100% - '+ height + 'px)');
  this.calendar._renderDisplayMode();
};

scout.CalendarLayout.prototype.preferredLayoutSize = function($comp) {
  return scout.graphics.getSize($comp);
};

scout.CalendarLayout.prototype.invalidate = function($comp) {
  this.calendar.layoutSize();
  this.calendar.layoutLabel();
  this.calendar.layoutComponents();
};
