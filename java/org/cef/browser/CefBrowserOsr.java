// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefScreenInfo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * This class represents an off-screen rendered browser.
 * The visibility of this class is "package". To create a new
 * CefBrowser instance, please use CefBrowserFactory.
 */
public class CefBrowserOsr extends CefBrowser_N implements CefRenderHandler {
  private boolean justCreated_ = false;
  protected Rectangle browser_rect_ = new Rectangle(0, 0, 1, 1); // Work around CEF issue #1437.
  private Point screenPoint_ = new Point(0, 0);
  private double scaleFactor_ = 1.0;
  private int depth = 32;
  private int depth_per_component = 8;
  private boolean isTransparent_;

  private CopyOnWriteArrayList<Consumer<CefPaintEvent>> onPaintListeners = new CopyOnWriteArrayList<>();

  CefBrowserOsr(CefClient client, String url, boolean transparent, CefRequestContext context,
      CefBrowserSettings settings) {
    this(client, url, transparent, context, null, null, settings);
  }

  private CefBrowserOsr(CefClient client, String url, boolean transparent,
      CefRequestContext context, CefBrowserOsr parent, Point inspectAt,
      CefBrowserSettings settings) {
    super(client, url, context, parent, inspectAt, settings);
    isTransparent_ = transparent;
  }

  @Override
  public void createImmediately() {
    justCreated_ = true;
    // Create the browser immediately.
    createBrowserIfRequired(false);
  }

  @Override
  public CefRenderHandler getRenderHandler() {
    return this;
  }

  @Override
  protected CefBrowser_N createDevToolsBrowser(CefClient client, String url,
      CefRequestContext context, CefBrowser_N parent, Point inspectAt) {
    return null;
  }

  @Override
  public Rectangle getViewRect(CefBrowser browser) {
    return browser_rect_;
  }

  @Override
  public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
    Point screenPoint = new Point(screenPoint_);
    screenPoint.translate(viewPoint.x, viewPoint.y);
    return screenPoint;
  }

  @Override
  public void onPopupShow(CefBrowser browser, boolean show) {
  }

    @Override
    public void onPopupSize(CefBrowser browser, Rectangle size) {
    }

  @Override
  public void addOnPaintListener(Consumer<CefPaintEvent> listener) {
    onPaintListeners.add(listener);
  }

  @Override
  public void setOnPaintListener(Consumer<CefPaintEvent> listener) {
    onPaintListeners.clear();
    onPaintListeners.add(listener);
  }

  @Override
  public void removeOnPaintListener(Consumer<CefPaintEvent> listener) {
    onPaintListeners.remove(listener);
  }

  @Override
  public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width,
      int height) {
    if (!onPaintListeners.isEmpty()) {
      CefPaintEvent paintEvent = new CefPaintEvent(browser, popup, dirtyRects, buffer, width, height);
      for (Consumer<CefPaintEvent> l : onPaintListeners) {
        l.accept(paintEvent);
      }
    }
  }

  @Override
  public boolean onCursorChange(CefBrowser browser, final int cursorType) {
    return true;
  }

  // private static final class SyntheticDragGestureRecognizer extends
  // DragGestureRecognizer {
  // public SyntheticDragGestureRecognizer(Component c, int action, MouseEvent
  // triggerEvent) {
  // super(new DragSource(), c, action);
  // appendEvent(triggerEvent);
  // }

  // protected void registerListeners() {}

  // protected void unregisterListeners() {}
  // };

  // private static int getDndAction(int mask) {
  // // Default to copy if multiple operations are specified.
  // int action = DnDConstants.ACTION_NONE;
  // if ((mask & CefDragData.DragOperations.DRAG_OPERATION_COPY)
  // == CefDragData.DragOperations.DRAG_OPERATION_COPY) {
  // action = DnDConstants.ACTION_COPY;
  // } else if ((mask & CefDragData.DragOperations.DRAG_OPERATION_MOVE)
  // == CefDragData.DragOperations.DRAG_OPERATION_MOVE) {
  // action = DnDConstants.ACTION_MOVE;
  // } else if ((mask & CefDragData.DragOperations.DRAG_OPERATION_LINK)
  // == CefDragData.DragOperations.DRAG_OPERATION_LINK) {
  // action = DnDConstants.ACTION_LINK;
  // }
  // return action;
  // }

  @Override
  public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
    return true;
  }

  @Override
  public void updateDragCursor(CefBrowser browser, int operation) {
  }

  private void createBrowserIfRequired(boolean hasParent) {
    long windowHandle = 0;
    if (getNativeRef("CefBrowser") == 0) {
      if (getParentBrowser() != null) {
        createDevTools(getParentBrowser(), getClient(), windowHandle, true, isTransparent_,
            getInspectAt());
      } else {
        createBrowser(getClient(), windowHandle, getUrl(), true, isTransparent_,
            getRequestContext());
      }
    } else if (hasParent && justCreated_) {
      notifyAfterParentChanged();
      setFocus(true);
      justCreated_ = false;
    }
  }

  private void notifyAfterParentChanged() {
    // With OSR there is no native window to reparent but we still need to send the
    // notification.
    getClient().onAfterParentChanged(this);
  }

  @Override
  public boolean getScreenInfo(CefBrowser browser, CefScreenInfo screenInfo) {
    screenInfo.Set(scaleFactor_, depth, depth_per_component, false, browser_rect_.getBounds(),
        browser_rect_.getBounds());

    return true;
  }

  @Override
  public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
    return null;
  }
}
