from django.urls import path
from .views import recommend, current_price, price_change

urlpatterns = [
    path("recommend", recommend, name="recommend"),
    path("current_price", current_price, name="current_price"),
    path("price_change", price_change, name="price_change"),
]