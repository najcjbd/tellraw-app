# [E1] 头盔 | 期望匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.armor.head 0 diamond_helmet 1
execute if entity @s[hasitem={item=diamond_helmet,location=slot.armor.head}] run scoreboard players set #e1s tt 1
execute unless entity @s[hasitem={item=diamond_helmet,location=slot.armor.head}] run scoreboard players set #e1s tt 0
execute if entity @s[hasitem={item=diamond_helmet,location=slot.armor.head}] run scoreboard players set #e1 tt 1
execute unless entity @s[hasitem={item=diamond_helmet,location=slot.armor.head}] run scoreboard players set #e1 tt 0
