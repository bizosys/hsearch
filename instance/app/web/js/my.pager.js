(function ($) {
  function MyPager(dataview,$container,options) {
    var $status;
    var defaultOptions = {}
    var onPaging = new Slick.Event();
    
    function init() {
      defaultOptions = options;
      constructPagerUI();
      updatePager(options);
    }

    function getNavState() {
      var cannotLeaveEditMode = !Slick.GlobalEditorLock.commitCurrentEdit();
      var pagingInfo = defaultOptions;
      var lastPage = pagingInfo.totalPages - 1;

      return {
        canGotoFirst: !cannotLeaveEditMode && pagingInfo.pageSize != 0 && pagingInfo.pageNum > 0,
        canGotoLast: !cannotLeaveEditMode && pagingInfo.pageSize != 0 && pagingInfo.pageNum != lastPage,
        canGotoPrev: !cannotLeaveEditMode && pagingInfo.pageSize != 0 && pagingInfo.pageNum > 0,
        canGotoNext: !cannotLeaveEditMode && pagingInfo.pageSize != 0 && pagingInfo.pageNum < lastPage,
        pagingInfo: pagingInfo
      }
    }

    function setPageSize(n) {
      setPagingOptions({pageSize: n});
    }
    
    function setDefaultOptions(options){
    	if(options.pageSize !== undefined){
    		defaultOptions.pageSize = options.pageSize; 
    	}
    	if(options.pageNum !== undefined){
    		defaultOptions.pageNum = options.pageNum; 
    	}
    	if(options.pageSize !== undefined){
    		defaultOptions.totalRows = options.totalRows; 
    	}
    	if(options.pageSize !== undefined){
    		defaultOptions.totalPages = options.totalPages; 
    	}
    	updatePager(defaultOptions);
    }
    
    function getDefaultOptions(){
    	return defaultOptions;
    }
    
    function setPagingOptions(options){
    	setDefaultOptions(options)
    	onPaging.notify(defaultOptions, null, self);
    }

    function gotoFirst() {
      if (getNavState().canGotoFirst) {
        setPagingOptions({pageNum: 0});
      }
    }

    function gotoLast() {
      var state = getNavState();
      if (state.canGotoLast) {
        setPagingOptions({pageNum: state.pagingInfo.totalPages - 1});
      }
    }

    function gotoPrev() {
      var state = getNavState();
      if (state.canGotoPrev) {
        setPagingOptions({pageNum: state.pagingInfo.pageNum - 1});
      }
    }

    function gotoNext() {
      var state = getNavState();
      if (state.canGotoNext) {
        setPagingOptions({pageNum: state.pagingInfo.pageNum + 1});
      }
    }

    function constructPagerUI() {
      $container.empty();

      var $nav = $("<span class='slick-pager-nav' />").appendTo($container);
      $status = $("<span class='slick-pager-status' />").appendTo($container);
      var icon_prefix = "<span class='ui-state-default ui-corner-all ui-icon-container'><span class='ui-icon ";
      var icon_suffix = "' /></span>";

      $(icon_prefix + "ui-icon-seek-first" + icon_suffix)
          .click(gotoFirst)
          .appendTo($nav);

      $(icon_prefix + "ui-icon-seek-prev" + icon_suffix)
          .click(gotoPrev)
          .appendTo($nav);

      $(icon_prefix + "ui-icon-seek-next" + icon_suffix)
          .click(gotoNext)
          .appendTo($nav);

      $(icon_prefix + "ui-icon-seek-end" + icon_suffix)
          .click(gotoLast)
          .appendTo($nav);

      $container.find(".ui-icon-container")
          .hover(function () {
            $(this).toggleClass("ui-state-hover");
          });

      $container.children().wrapAll("<div class='slick-pager' />");
    }


    function updatePager(pagingInfo) {
      var state = getNavState();

      $container.find(".slick-pager-nav span").removeClass("ui-state-disabled");
      if (!state.canGotoFirst) {
        $container.find(".ui-icon-seek-first").addClass("ui-state-disabled");
      }
      if (!state.canGotoLast) {
        $container.find(".ui-icon-seek-end").addClass("ui-state-disabled");
      }
      if (!state.canGotoNext) {
        $container.find(".ui-icon-seek-next").addClass("ui-state-disabled");
      }
      if (!state.canGotoPrev) {
        $container.find(".ui-icon-seek-prev").addClass("ui-state-disabled");
      }

      if (pagingInfo.pageSize == 0) {
        $status.text("No pages to display");
      } else {
        $status.text("Showing page " + (pagingInfo.pageNum + 1) + " of " + pagingInfo.totalPages);
      }
    }

    $.extend(this, {
    	"onPaging": onPaging,
    	"setDefaultOptions":setDefaultOptions,
    	"getDefaultOptions":getDefaultOptions
    });
    init();
  }

  // Slick.Controls.Pager
  $.extend(true, window, { Slick:{ Controls:{ Pager:MyPager }}});
})(jQuery);
