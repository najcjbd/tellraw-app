# [Z1] target=* 是否合法（我们舍弃它） | 只看实测值
scoreboard objectives add tt dummy
clear @s
item replace entity @s hotbar.5 with minecraft:diamond 1
execute if items entity @s hotbar.5 minecraft:diamond run scoreboard players set #z1s tt 1
execute unless items entity @s hotbar.5 minecraft:diamond run scoreboard players set #z1s tt 0
execute if items entity * minecraft:diamond run scoreboard players set #z1 tt 1
execute unless items entity * minecraft:diamond run scoreboard players set #z1 tt 0
