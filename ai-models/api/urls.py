from django.urls import path
from .views import recommend, current_price, price_change, load_current_price_view

urlpatterns = [
    path("recommend", recommend, name="recommend"),
    path("current_price", current_price, name="current_price"),
    path("price_change", price_change, name="price_change"),
    path("load_current_price", load_current_price_view, name="load_current_price_view")
]