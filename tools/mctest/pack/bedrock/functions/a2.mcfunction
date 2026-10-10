# [A2] air：头盔有物时应不命中 | 只看实测值
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.armor.head 0 diamond_helmet 1
execute if entity @s[hasitem={item=diamond_helmet,location=slot.armor.head}] run scoreboard players set #a2s tt 1
execute unless entity @s[hasitem={item=diamond_helmet,location=slot.armor.head}] run scoreboard players set #a2s tt 0
execute if entity @s[hasitem={item=air,location=slot.armor.head}] run scoreboard players set #a2 tt 1
execute unless entity @s[hasitem={item=air,location=slot.armor.head}] run scoreboard players set #a2 tt 0
