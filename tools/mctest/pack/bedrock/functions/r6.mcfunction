# [R6] 单槽位 !2（已测：命中） | 只看实测值
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.armor.head 0 diamond_helmet 1
execute if entity @s[hasitem={item=diamond_helmet,location=slot.armor.head}] run scoreboard players set #r6s tt 1
execute unless entity @s[hasitem={item=diamond_helmet,location=slot.armor.head}] run scoreboard players set #r6s tt 0
execute if entity @s[hasitem={item=diamond_helmet,location=slot.armor.head,slot=!2}] run scoreboard players set #r6 tt 1
execute unless entity @s[hasitem={item=diamond_helmet,location=slot.armor.head,slot=!2}] run scoreboard players set #r6 tt 0
